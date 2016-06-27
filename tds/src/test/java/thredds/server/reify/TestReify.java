package thredds.server.reify;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NotJenkins;
import ucar.unidata.util.test.category.NotTravis;

@RunWith(SpringJUnit4ClassRunner.class)
@Category({NotJenkins.class, NotTravis.class})
public class TestReify extends UnitTestCommon
{
    static protected final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constants

    static protected final String TESTINPUTDIR = "/testfiles";
    static protected final String BASELINEDIR = "/TestReify/baseline";

    static protected final String FAKEURLPREFIX = "/reify";


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
        protected String baselinepath;

        protected TestCase(String dataset)
        {
            this.dataset = dataset;
            this.testinputpath = canonjoin(this.inputroot, dataset);
            this.baselinepath = canonjoin(this.baselineroot, dataset);
        }

        String makeurl()
        {
            String u = canonjoin(FAKEURLPREFIX, canonjoin(TESTINPUTDIR, dataset));
            return u;
        }

        public String toString()
        {
            return dataset;
        }
    }

    static protected void setTESTDIRS(String... dirs)
    {
        ReifyController.TESTDIRS = dirs;
    }

    //////////////////////////////////////////////////
    // Test cases
    static TestCase[] TESTCASES = {
            new TestCase("abc"),
    };

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
        setTESTDIRS("");
        TestCase.setRoots(TESTINPUTDIR, BASELINEDIR);
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

        RequestBuilder rb = MockMvcRequestBuilders
                .get(url)
                .servletPath(url);
        MvcResult result = this.mockMvc.perform(rb).andReturn();

        // Collect the output
        MockHttpServletResponse res = result.getResponse();

        byte[] byteresult = res.getContentAsByteArray();

        // Convert the raw output to a string
        String sresult = new String(byteresult, UTF8);
        if(prop_visual)
            visual("TestReify", sresult);
        //if(prop_diff) { //compare with baseline
        //    Assert.assertTrue("***Fail", same(getTitle(), test.baseline, sresult));
    }

}
