/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.reify;

import dap4.core.util.DapUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.core.TdsRequestedDataset;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CancelTaskImpl;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * Local File Materialization for Jupyter
 * <p>
 * handles /reify/*
 */
@Controller
@RequestMapping("/reify")
public class ReifyController
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Static variables

    static protected org.slf4j.Logger log;

    static public String[] TESTDIRS = null;
    static public String TESTROOTPATH = "c:/Temp";

    static {
        log = org.slf4j.LoggerFactory.getLogger(ReifyController.class);
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected HttpServletRequest req = null;
    protected HttpServletResponse res = null;
    protected Parameters params = null;
    protected String rootpath = null;
    protected boolean initialized = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public ReifyController()
            throws ServletException
    {
        if(!initialized)
            init(); // Do no know how to get spring to invoke when mocking.
    }

    //////////////////////////////////////////////////
    // Servlet API (Selected)

    public void init()
            throws ServletException
    {
        if(initialized)
            return;
        initialized = true;
        org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
        logServerStartup.info(getClass().getName() + " initialization start");
        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        this.rootpath = TESTROOTPATH;
    }

    //////////////////////////////////////////////////
    // Controller entry point

    @RequestMapping("**")
    public Object doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException
    {
        this.req = req;
        this.res = res;

        String reqpath = TdsPathUtils.extractPath(req, "reify/");
        if(reqpath == null)
            return senderror(res.SC_BAD_REQUEST);

        try {
            this.params = new Parameters().process(req, reqpath, this.rootpath);
        } catch (IOException ioe) {
            senderror(res.SC_BAD_REQUEST, ioe);
        }

        int codep[] = new int[1];
        File file = resolve(req, res, reqpath, codep);
        if(file == null)
            return senderror(codep[0], reqpath);

        // Get NetcdfFile object
        NetcdfFile ncfile = null;
        try {
            CancelTaskImpl cancel = new CancelTaskImpl();
            ncfile = NetcdfDataset.openFile(file.getAbsolutePath(), cancel);
        } catch (IOException ioe) {
            return senderror(HttpServletResponse.SC_FORBIDDEN, file.getAbsolutePath());
        }
        String returnfile = null;
        switch (params.format) {
        case NETCDF3:
            try {
                returnfile = makeNetcdf3(ncfile);
            } catch (IOException ioe) {
                senderror(res.SC_BAD_REQUEST, ioe);
            }
            break;
        case NETCDF4:
            try {
                returnfile = makeNetcdf4(ncfile);
            } catch (IOException ioe) {
                senderror(res.SC_BAD_REQUEST, ioe);
            }
            break;
        case DAP2:
        case DAP4:
            return senderror(HttpServletResponse.SC_NOT_IMPLEMENTED,
                    String.format("%s: return format %s not implemented",
                            file.getAbsolutePath(),
                            params.format.getName()));
        }
        // Return the absolutepath of the file as the content
        if(returnfile == null)
            return senderror(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        res.setContentType(ContentType.text.getContentHeader());
        try {
            ServletOutputStream out = res.getOutputStream();
            PrintWriter pw = new PrintWriter(out);
            pw.printf(returnfile);
            pw.close();
            out.flush();
        } catch (IOException ioe) {
            return senderror(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    //////////////////////////////////////////////////
    // Reifiers

    protected String
    makeNetcdf4(NetcdfFile ncfile)
            throws IOException
    {
        try {
            CancelTaskImpl cancel = new CancelTaskImpl();
            FileWriter2 writer = new FileWriter2(ncfile, params.fullpath,
                    NetcdfFileWriter.Version.netcdf4,
                    params.chunking);
            writer.getNetcdfFileWriter().setLargeFile(true);
            NetcdfFile ncfileOut = writer.write(cancel);
            if(ncfileOut != null) ncfileOut.close();
            cancel.setDone(true);
            return params.fullpath;
        } catch (IOException ioe) {
            throw ioe; // temporary
        }
    }

    protected String
    makeNetcdf3(NetcdfFile ncfile)
            throws IOException
    {
        try {
            CancelTaskImpl cancel = new CancelTaskImpl();
            FileWriter2 writer = new FileWriter2(ncfile, params.fullpath,
                    NetcdfFileWriter.Version.netcdf3,
                    params.chunking);
            writer.getNetcdfFileWriter().setLargeFile(true);
            NetcdfFile ncfileOut = writer.write(cancel);
            if(ncfileOut != null) ncfileOut.close();
            cancel.setDone(true);
            return params.fullpath;
        } catch (IOException ioe) {
            throw ioe; // temporary
        }
    }

    //////////////////////////////////////////////////
    // Utilities

    /**
     * Generate an error based on the parameters
     *
     * @param httpcode 0=>no code specified
     */

    protected Object
    senderror(int httpcode)
    {
        return senderror(httpcode, (String) null);
    }

    /**
     * Generate an error based on the parameters
     *
     * @param httpcode 0=>no code specified
     * @param t        exception that caused the error; may not be null
     */

    protected Object
    senderror(int httpcode, Throwable t)
    {
        return senderror(httpcode, t.getMessage());
    }

    /**
     * Generate an error based on the parameters
     *
     * @param httpcode 0=>no code specified
     * @param msg      additional info; may be null
     */
    ;

    protected Object
    senderror(int httpcode, String msg)
    {
        if(httpcode == 0) httpcode = HttpServletResponse.SC_BAD_REQUEST;
        if(msg == null)
            msg = "";
        try {
            this.res.sendError(httpcode, msg);
        } catch (IOException ioe) {
        }
        return null;
    }

    protected File
    resolve(HttpServletRequest req, HttpServletResponse res, String reqpath, int[] codep)
    {
        File file = null;
        if(TESTDIRS != null) {
            for(String s : TESTDIRS) {
                String path = DapUtil.canonjoin(s, reqpath);
                File f = new File(path);
                if(f.exists()) {
                    file = f;
                    break;
                }
            }
        } else {
            if(!TdsRequestedDataset.resourceControlOk(req, res, reqpath)) {
                codep[0] = res.SC_UNAUTHORIZED;
                return null;
            }
            file = TdsRequestedDataset.getFile(reqpath);
            if(file == null || !file.exists()) {
                codep[0] = res.SC_NOT_FOUND;
                return null;
            }
        }
        return file;
    }

}
