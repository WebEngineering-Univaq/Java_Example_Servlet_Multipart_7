package it.univaq.f4i.iw.examples;

import it.univaq.f4i.iw.framework.result.HTMLResult;
import it.univaq.f4i.iw.framework.security.SecurityHelpers;
import it.univaq.f4i.iw.framework.utils.ServletHelpers;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 *
 * @author Giuseppe Della Penna
 *
 * Warning: this code requires JEE 6+ and the multipart-config configuration
 * element added to the web.xml (or the corresponding annotation on this
 * servlet)
 *
 */
public class Uploadami extends HttpServlet {

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HTMLResult result = new HTMLResult(getServletContext());
        result.setTitle("Upload me!");
        result.appendToBody("<p>Hello!</p>");
        result.appendToBody("<form method=\"post\" action=\"upload\" enctype='multipart/form-data'>");
        result.appendToBody("<p>Which file do you want to send me?");
        result.appendToBody("<input type=\"file\" name=\"f1\"/></p>");
        result.appendToBody("<p>Describe this file: ");
        result.appendToBody("<input type=\"text\" name=\"d\"/></p>");
        result.appendToBody("</p><input type=\"submit\" name=\"s\" value=\"Go!\"/></p>");
        result.appendToBody("</form>");
        result.activate(request, response);
    }

    private void action_upload(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String d = request.getParameter("d");
        //we could also get the other form fields as parts. However, getParameter works in this case, and it is easier to use!
        Part p = request.getPart("f1");
        //getPart returns null if the part does not exist
        if (p != null) {
            String name = p.getSubmittedFileName(); //filename should be sanitized
            String contentType = p.getContentType();
            long size = p.getSize();
            if (size > 0 && name != null && !name.isBlank()) {
                //sanitize filename
                name = SecurityHelpers.sanitizeFilename(name);
                //Path target = Paths.get(getServletContext().getRealPath("") + File.separatorChar + "uploads" + File.separatorChar + name);
                //safer: getRealPath may not work in all contexts/configurations
                Path target = Paths.get(getServletContext().getInitParameter("uploads.directory") + File.separatorChar + name);
                //handle files with the same name in the output directory
                int guess = 0;
                while (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    target = Paths.get(getServletContext().getInitParameter("uploads.directory") + File.separatorChar + (++guess) + "_" + name);
                }
                //or, to create a completely new filename without extension, you can use
                //target = File.createTempFile("upload_", "", new File(getServletContext().getInitParameter("uploads.directory"))).toPath();

                //if you call the Part.write method, remember that paths passed to this method are relative to the (temp) location indicated in the multipartconfig
                Files.copy(p.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING); //nio utility. Otherwise, use a buffer and copy from inputstream to fileoutputstream

                //read the file back (just to check...)
                byte[] buffer = new byte[10];
                int read = 0;
                try ( InputStream is = new FileInputStream(target.toFile())) {
                    read = is.read(buffer);
                }

                HTMLResult result = new HTMLResult(getServletContext());
                result.setTitle("Upload successful!");
                result.appendToBody("<p><strong>Uploaded file:</strong> " + HTMLResult.sanitizeHTMLOutput(name) + "</p>");
                result.appendToBody("<p><strong>Uploaded file type:</strong> " + HTMLResult.sanitizeHTMLOutput(contentType) + "</p>");
                result.appendToBody("<p><strong>Uploaded file size:</strong> " + size + "</p>");
                result.appendToBody("<p><strong>Description:</strong> " + HTMLResult.sanitizeHTMLOutput(d) + "</p>");
                result.appendToBody("<p><strong>Local file:</strong> " + HTMLResult.sanitizeHTMLOutput(target.toString()) + "</p>");
                result.appendToBody("<p><strong>First " + read + " file bytes:</strong> " + HTMLResult.sanitizeHTMLOutput(Arrays.toString(buffer)) + "</p>");
                result.activate(request, response);
            } else {
                ServletHelpers.handleError("File truncated", request, response, getServletContext());
            }
        } else {
            ServletHelpers.handleError("File unavailable", request, response, getServletContext());
        }
    }

    //this file upload technique is available from JEE 6 only
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        try {
            //if the request is empty or is not multipart encoded, calling getPart() would raise a ServletException!
            if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
                //we could also get the other form fields as parts. However, getParameter works in this case, and it is easier to use!
                Part p = request.getPart("f1");
                //getPart returns null if the part does not exist
                if (p != null) {
                    action_upload(request, response);
                } else {
                    ServletHelpers.handleError("File unavailable", request, response, getServletContext());
                }
            } else {
                action_default(request, response);
            }
        } catch (Exception ex) {
            request.setAttribute("exception", ex);
            ServletHelpers.handleError(request, response, getServletContext());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);

    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "A kind servlet";

    }// </editor-fold>
}
