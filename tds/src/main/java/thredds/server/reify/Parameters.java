/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package thredds.server.reify;


import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Process an HttpRequest to extract reification info
 */
class Parameters
{
    //////////////////////////////////////////////////
    // Constants

    static final String TMPPREFIX = "tmp";

    static protected org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(ReifyController.class);

    //////////////////////////////////////////////////
    // Type Decls

    static public enum ReturnFormat
    {
        NETCDF3("netcdf3", "nc3"),
        NETCDF4("netcdf4", "nc4"),
        DAP2("dap2", "dods"),
        DAP4("dap4", "dap");

        private String name;
        private String extension;

        public final String getName()
        {
            return this.name;
        }

        public final String getExtension()
        {
            return this.extension;
        }

        ReturnFormat(String name, String ext)
        {
            this.name = name;
            this.extension = ext;
        }


        static public final ReturnFormat DEFAULTRETURNFORMAT = NETCDF4;

        static public ReturnFormat getformat(String fmt)
        {
            if(fmt == null) return null;
            for(ReturnFormat rf : ReturnFormat.values()) {
                if(fmt.equalsIgnoreCase(rf.getName())) return rf;
            }
            return null;
        }
    }

    //////////////////////////////////////////////////
    // Instance variables (Allow direct access)

    // Inputs
    public String rootpath = null; // root dir of where to store files
    public String relpath = null; // file being requested

    Map<String, String[]> params = null;

    // Outputs
    public ReturnFormat format = null;

    public String path = null; // Where file is to be stored
    public String fullpath = null;

    public int deflatelevel = 9;
    public boolean shuffle = false;

    public boolean testing = false;

    public Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;
    public Nc4Chunking chunking = new Nc4ChunkingDefault();

    //////////////////////////////////////////////////
    // Constructor(s)

    public Parameters()
            throws IOException
    {
    }


    //////////////////////////////////////////////////
    // API

    public Parameters
    process(HttpServletRequest req, String reqpath, String rootpath)
            throws IOException
    {
        this.rootpath = rootpath;
        if(this.rootpath.length() == 0)
            throw new IOException("Null root path");

        int i = reqpath.lastIndexOf('.');
        if(i > 0)
            reqpath = reqpath.substring(0, i);
        reqpath = reqpath.replace('\\', '/');
        this.relpath = reqpath;

        this.params = req.getParameterMap();

        this.testing = getparam("testing") != null;

        // Return Format
        String s = getparam("format");
        ReturnFormat fmt = ReturnFormat.getformat(s);
        if(fmt == null) fmt = ReturnFormat.DEFAULTRETURNFORMAT;
        this.format = fmt;

        // Relative path (filename will be synthesized)
        s = getparam("path");
        this.path = canonpath(s, true);

        // Make sure path exists
        StringBuilder fullpath = new StringBuilder(this.rootpath);
        fullpath.append("/");
        fullpath.append(this.path);
        File fdir = new File(fullpath.toString());

        // Synthesize a file name
        StringBuilder buf = new StringBuilder();
        File f;
        if(this.testing) {
            buf.append(this.rootpath);
            buf.append('/');
            buf.append(path);
            buf.append('/');
            buf.append(this.relpath);
            buf.append('.');
            buf.append(fmt.getExtension());
            String p = buf.toString().replace('\\', '/');
            f = new File(p);
            f.delete();
        } else {
            f = File.createTempFile(TMPPREFIX, "." + fmt.getExtension(), fdir);
        }
        this.fullpath = canonpath(f.getAbsolutePath(), false);
        // Make sure the file dirs is created
        f.getParentFile().mkdirs();

        // Make sure this is writeable
        boolean b = f.createNewFile();
        if(!b)
            throw new IOException("Cannot create output file: "+this.fullpath);

        // Get misc. other params
        s = getparam("shuffle");
        if(s != null) this.shuffle = true;
        s = getparam("deflatelevel");
        try {
            int level = Integer.parseInt(s);
            this.deflatelevel = level;
        } catch (NumberFormatException nfe) {
        }

        // TBD: get chunking

        return this;
    }

    //////////////////////////////////////////////////
    // Utilities

    protected String
    getparam(String key)
    {


        if(this.params == null) return null;
        String[] values = this.params.get(key);
        if(values == null || values.length == 0) return null;
        return values[0];
    }

    static public String
    canonpath(String s, boolean relative)
    {
        if(s == null) s = "";
        s = s.replace('\\', '/');
        if(relative && s.startsWith("/")) s = s.substring(1);
        if(s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}


