package thredds.server.reify;

import dap4.core.util.DapUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import ucar.nc2.NetcdfFile;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NotJenkins;
import ucar.unidata.util.test.category.NotTravis;

import javax.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;

@Category({NotJenkins.class, NotTravis.class})
public class TestReify extends UnitTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Test cases
    static TestCase[] TESTCASES = {
            new TestCase("nc3/test_atomic_types.nc",
                    new String[]{"format=netcdf3", "path=testreify"},
                    "c:/Temp/testreify/nc3/test_atomic_types.nc3"),
            new TestCase("nc4/test_atomic_types_simple.nc",
                                new String[]{"format=netcdf4", "path=testreify"},
                                "c:/Temp/testreify/nc4/test_atomic_types_simple.nc4"),
    };

    //////////////////////////////////////////////////
    // Constants
    static protected final String URLPREFIX = "/reify";
    static protected final String TESTINPUTSUFFIX = "/src/test/resources/thredds/server/reify";
    static protected final String RESOURCEDIR
            = DapUtil.canonicalpath(System.getProperty("user.dir")) + TESTINPUTSUFFIX;
    static protected final String TESTINPUTDIR = RESOURCEDIR + "/testfiles";
    static protected final String BASELINEDIR = RESOURCEDIR + "/baseline";

    //////////////////////////////////////////////////
    // Type Decls

    static class TestCase
    {
        static String inputroot = null;
        static String baselineroot = null;

        static public void
        setRoots(String input, String baseline)
        {
            inputroot = input;
            baselineroot = baseline;
        }

        protected String dataset;
        protected String testinputpath;
        protected String baseline;
        protected Map<String, String> params = new HashMap<>();

        protected TestCase(String dataset, String[] params, String baseline)
        {
            this.dataset = dataset;
            this.testinputpath = canonjoin(this.inputroot, dataset);
            this.baseline = baseline;
            for(int i = 0; i < params.length; i++) {
                String[] pieces = params[i].split("[=]");
                if(pieces.length == 1)
                    this.params.put(pieces[0], "");
                else
                    this.params.put(pieces[0], pieces[1]);
            }
            this.params.put("testing","true");
        }

        String makeurl()
        {
            String u = canonjoin(URLPREFIX, this.dataset);
            return u;
        }

        String getBaseline()
        {
            return this.baseline;
        }

        String getTestpath()
        {
            String u = canonjoin(TESTINPUTDIR, this.dataset);
            return u;
        }

        Map<String, String> getParams()
        {
            return this.params;
        }

        public String toString()
        {
            return this.dataset;
        }
    }

    static protected void setTESTDIRS(String... dirs)
    {
        ReifyController.TESTDIRS = dirs;
    }

    //////////////////////////////////////////////////
    // Instance variables

    @Autowired
    protected WebApplicationContext wac;
    protected String resourceroot = null;
    protected MockMvc mockMvc = null;


    //////////////////////////////////////////////////
    // Constructor(s)

    public TestReify()
    {
    }

    //////////////////////////////////////////////////
    // Junit test methods

    @Before
    public void setup()
            throws ServletException
    {
        StandaloneMockMvcBuilder mvcbuilder =
                MockMvcBuilders.standaloneSetup(new ReifyController());
        Validator v = new Validator()
        {
            public boolean supports(Class<?> clazz)
            {
                return true;
            }

            public void validate(Object target, Errors errors)
            {
                return;
            }
        };
        mvcbuilder.setValidator(v);
        this.mockMvc = mvcbuilder.build();
        setTESTDIRS(TESTINPUTDIR);
        TestCase.setRoots(TESTINPUTDIR, BASELINEDIR);
        try {
            // Registers Nc4Iosp in front of all the other IOSPs already registered in NetcdfFile.<clinit>().
            // Crucially, this means that we'll try to open a file with Nc4Iosp before we try it with H5iosp.
            NetcdfFile.registerIOProvider(Nc4Iosp.class);
        } catch (IllegalAccessException | InstantiationException e) {
            log.error("CdmInit: Unable to register IOSP: " + Nc4Iosp.class.getCanonicalName(), e);
        }
    }

    @Test
    public void testReify()
            throws Exception
    {
        for(int i = 0; i < TESTCASES.length; i++) {
            doOneTest(TESTCASES[i]);
        }
    }

    //////////////////////////////////////////////////
    // Primary test method
    void
    doOneTest(TestCase test)
            throws Exception
    {
        String url = test.makeurl();

        System.out.println("Testcase: get: " + url);

        MockHttpServletRequestBuilder rb = MockMvcRequestBuilders
                .get(url)
                .servletPath(url);
        for(Map.Entry<String, String> entry : test.getParams().entrySet()) {
            rb.param(entry.getKey(), entry.getValue());
        }
        MvcResult result = this.mockMvc.perform(rb).andReturn();

        // Collect the output
        MockHttpServletResponse res = result.getResponse();

        byte[] byteresult = res.getContentAsByteArray();

        // Convert the raw output to a string
        String sresult = new String(byteresult, UTF8);
        if(prop_visual)
            visual("TestReify", sresult);

        if(prop_diff) { //compare with baseline
            Assert.assertTrue("***Fail", same(getTitle(), test.baseline, sresult));
        }

    }
}
