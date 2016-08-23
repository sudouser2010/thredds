/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *  See the LICENSE file for more information.
 */

package thredds.server.fileserver;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;
import thredds.core.TdsRequestedDataset;
import thredds.servlet.ServletUtil;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;

/**
 * HTTP File Serving
 *
 * handles /fileServer/*
 */
@Controller
@RequestMapping("/fileServer")
public class FileServerController implements LastModified {
  protected static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileServerController.class);

  public long getLastModified(HttpServletRequest req) {
    String reqPath = TdsPathUtils.extractPath(req, "fileServer/");
    if (reqPath == null) return -1;

    File file = getFile( reqPath);
    if (file == null)
      return -1;

    return file.lastModified();
  }

  @RequestMapping("**")
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    String reqPath = TdsPathUtils.extractPath(req, "fileServer/");
    if (reqPath == null) return;

    if (!TdsRequestedDataset.resourceControlOk(req, res, reqPath)) {  // LOOK or process in TdsRequestedDataset.getFile ??
      return;
    }

    File file = getFile( reqPath);
    ServletUtil.returnFile(null, req, res, file, null);
  }

  private File getFile(String reqPath) {
    if (reqPath == null) return null;

    File file = TdsRequestedDataset.getFile(reqPath);
    if (file == null)
      return null;
    if (!file.exists())
      return null;

    return file;
  }

}
