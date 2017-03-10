/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EnrollFingerprint;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.*;
import com.digitalpersona.onetouch.capture.event.*;
import com.digitalpersona.onetouch.processing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.FormBody;
import okhttp3.MultipartBody;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author AlejoDesktop
 */
public class Enrollment extends JApplet {
    
    private String apiComposition = "MTox";
    // Connection string format = jdbc:oracle:<drivertype>:<user>/<password>@<database>
    public static String connectionString = "jdbc:oracle:thin:analytics/qwerty@172.28.128.4:1521/XE";
    public static String TEMPLATE_PROPERTY = "template";
    private DPFPTemplate template;
    private DPFPCapture capturer = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment enroller = DPFPGlobal.getEnrollmentFactory().createEnrollment();

    @Override
    public void init(){
        initComponents();
        updateStatus();
        
       // Event listener actived when fingerprint template is ready
       this.addPropertyChangeListener(TEMPLATE_PROPERTY, new PropertyChangeListener(){
           public void propertyChange(PropertyChangeEvent evt){
               btnSave.setEnabled(template != null);
               if(evt.getNewValue() == evt.getOldValue()){
                   return;
               }
               if(template != null){
                   JOptionPane.showMessageDialog(
                           Enrollment.this,
                           "La huella capturada esta lista para ser guardada.",
                           "Captura y Registro de huellas",
                           JOptionPane.INFORMATION_MESSAGE
                   );
               }
           }
       });
    
        capturer.addDataListener(new DPFPDataAdapter(){
            @Override public void  dataAcquired(final DPFPDataEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        makeReport("La huella fue capturada.");
                        setPrompt("Escanear la misma huella de nuevo.");
                        process(e.getSample());
                    }
                });
            }
        });
        
        capturer.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            @Override public void  readerConnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        makeReport("Lector de huellas conectado.");
                    }
                });
            }
            @Override public void readerDisconnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        makeReport("Lector de huellas desconectado.");
                    }
                });
            }
        });
        
        capturer.addSensorListener(new DPFPSensorAdapter(){
            @Override public void fingerTouched(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        makeReport("El escaner de huellas fue tocado.");
                    }
                });
            }
            @Override public void fingerGone(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        makeReport("El dedo fue retirado del escaner de huellas.");
                    }
                });
            }
        });
        
        capturer.addImageQualityListener(new DPFPImageQualityAdapter(){
            @Override public void onImageQuality(final DPFPImageQualityEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(e.getFeedback().equals(DPFPCaptureFeedback.CAPTURE_FEEDBACK_GOOD)){
                            makeReport("La calidad de la huella escaneada es buena.");
                        }else{
                            makeReport("La calidad de la huella escaneada es pobre.");
                        }
                    }
                });
            }            
        });
    }
    
    protected void process(DPFPSample sample){
        // Show the fingerprint image.
        drawPicture(convertSampleToBitmap(sample));
        // Process the fingerprint and create a feature set form the enrollment purpose.
        DPFPFeatureSet features = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        
        // Check quality of the fingerprint and add to enroller if it's good.
        if(features != null) {
            try {
                makeReport("Creada la caracterización de la huella.");
                enroller.addFeatures(features);
            } catch (DPFPImageQualityException e) {}
            finally{
                updateStatus();
                
                // Check if a template has been created.
                switch(enroller.getTemplateStatus()){
                    case TEMPLATE_STATUS_READY:
                        stop();
                        setTemplate(enroller.getTemplate());
                        setPrompt("Huela lista para verificar.");
                        btnRead.setEnabled(false);
                        break;
                    
                    case TEMPLATE_STATUS_FAILED:
                        enroller.clear();
                        stop();
                        updateStatus();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(Enrollment.this,
                                "La huella capturada no es validad, repita el registro.",
                                "Captura y Registro de huellas", JOptionPane.ERROR_MESSAGE
                        );
                        start();
                        break;
                        
                }
            }
        }
        

    }
    
    public void start(){
        capturer.startCapture();
        setPrompt("Scanner de huellas listo a usarse, por favor introduzca la huella.");
    }
    
    public void stop(){
        capturer.stopCapture();
    }
    
    public void setStatus(String string){
        lblStatus.setText(string);
    }
    
    public void setPrompt(String string){
        txtConsole.setText(string);
    }
    
    public void makeReport(String string){
        txtLog.append(string + "\n");
    }
    
    public void drawPicture(Image image) {
        picFingerprint.setIcon(new ImageIcon(
                image.getScaledInstance(
                        picFingerprint.getWidth(), 
                        picFingerprint.getHeight(), 
                        Image.SCALE_DEFAULT
                )
        ));
    }
    
    public DPFPTemplate getTemplate() {
        return template;
    }
    
    public void setTemplate(DPFPTemplate template) {
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }
    
    private void updateStatus(){
        setStatus(String.format("Toma de huella requiridas: %1$s", enroller.getFeaturesNeeded()));
    }
    
    protected Image convertSampleToBitmap(DPFPSample sample){
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
    
    protected DPFPFeatureSet extractFeatures(DPFPSample sample, DPFPDataPurpose purpose){
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e){
            return null;
        }
    }
        
    String createJSON(String fingerprint, String personId, String fingerprintNumber){
        return "{"
                + "\"personId\": \"" + personId + "\","
                + "\"fingerprint\": \"" + fingerprint + "\","
                + "\"fingerprintNumber\": \"" + fingerprintNumber + "\""
                + "}";
    }
     
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanelBackground = new javax.swing.JPanel();
        picFingerprint = new javax.swing.JLabel();
        jPanelConsole = new javax.swing.JPanel();
        txtConsole = new javax.swing.JTextField();
        lblConsole = new javax.swing.JLabel();
        btnSave = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        jPanelLog = new javax.swing.JPanel();
        JScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        lblLog = new javax.swing.JLabel();
        btnRead = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();

        jPanelBackground.setBackground(new java.awt.Color(238, 241, 245));

        picFingerprint.setBackground(new java.awt.Color(255, 255, 255));
        picFingerprint.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jPanelConsole.setBackground(new java.awt.Color(255, 255, 255));

        txtConsole.setEditable(false);
        txtConsole.setForeground(new java.awt.Color(136, 136, 136));
        txtConsole.setText("Consola de estado");

        lblConsole.setForeground(new java.awt.Color(136, 136, 136));
        lblConsole.setText("Consola:");

        javax.swing.GroupLayout jPanelConsoleLayout = new javax.swing.GroupLayout(jPanelConsole);
        jPanelConsole.setLayout(jPanelConsoleLayout);
        jPanelConsoleLayout.setHorizontalGroup(
            jPanelConsoleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelConsoleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelConsoleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtConsole)
                    .addGroup(jPanelConsoleLayout.createSequentialGroup()
                        .addComponent(lblConsole)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelConsoleLayout.setVerticalGroup(
            jPanelConsoleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelConsoleLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(lblConsole)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtConsole, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        btnSave.setText("Guardar");
        btnSave.setEnabled(false);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancelar");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        jPanelLog.setBackground(new java.awt.Color(255, 255, 255));

        txtLog.setEditable(false);
        txtLog.setColumns(20);
        txtLog.setForeground(new java.awt.Color(168, 168, 168));
        txtLog.setRows(5);
        JScrollPane1.setViewportView(txtLog);

        lblLog.setForeground(new java.awt.Color(136, 136, 136));
        lblLog.setText("Estado:");

        javax.swing.GroupLayout jPanelLogLayout = new javax.swing.GroupLayout(jPanelLog);
        jPanelLog.setLayout(jPanelLogLayout);
        jPanelLogLayout.setHorizontalGroup(
            jPanelLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(JScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
                    .addGroup(jPanelLogLayout.createSequentialGroup()
                        .addComponent(lblLog)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelLogLayout.setVerticalGroup(
            jPanelLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblLog)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(JScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnRead.setText("Leer Huella");
        btnRead.setEnabled(false);
        btnRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReadActionPerformed(evt);
            }
        });

        lblStatus.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblStatus.setText("Intentos");

        javax.swing.GroupLayout jPanelBackgroundLayout = new javax.swing.GroupLayout(jPanelBackground);
        jPanelBackground.setLayout(jPanelBackgroundLayout);
        jPanelBackgroundLayout.setHorizontalGroup(
            jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBackgroundLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelBackgroundLayout.createSequentialGroup()
                        .addComponent(picFingerprint, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelConsole, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanelLog, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanelBackgroundLayout.createSequentialGroup()
                        .addComponent(lblStatus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnRead)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel)))
                .addContainerGap())
        );
        jPanelBackgroundLayout.setVerticalGroup(
            jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBackgroundLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBackgroundLayout.createSequentialGroup()
                        .addComponent(jPanelConsole, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelLog, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(picFingerprint, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addGroup(jPanelBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnSave)
                    .addComponent(btnRead)
                    .addComponent(lblStatus))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

    }// </editor-fold>                        

    private void btnReadActionPerformed(java.awt.event.ActionEvent evt) {                                        
        //init();
        //updateStatus();
        //start();
    }                                       

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {                                          
        System.exit(0);
    }                                         

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {                                        
        try {
            //byte[] fpdata = Base64.getEncoder().encode(fpglobal.toByteArray());
            byte[] fpdata = getTemplate().serialize();
            JSONParser parser = new JSONParser();
            
            String apiURL = "http://localhost:3012/api/v1/fingerprints";
            String response = new Requestor().postFingerprintMultipart(apiURL, fpdata, "1", "1");
                      
            //System.out.println(response);
            
            Object obj = parser.parse(response);

            JSONObject jsonObject = (JSONObject) obj;
            //System.out.println(jsonObject);
            
            // Get the code from the response
            long codigo = (Long) jsonObject.get("code");
            //System.out.println(codigo);

            // Get de message from response
            String mensaje = (String) jsonObject.get("message");
            //System.out.println(mensaje);
            
            if(codigo == 200){
                btnSave.setEnabled(false);
                JOptionPane.showMessageDialog(
                           Enrollment.this,
                           mensaje,
                           "Captura y Registro de huellas",
                           JOptionPane.INFORMATION_MESSAGE
                   );
            }
           
         
        } catch (Exception e) {
            System.out.println(e);
            JOptionPane.showMessageDialog(
                           Enrollment.this,
                           "Error guardando la huella, comuniquese con su administrador.",
                           "Captura y Registro de huellas",
                           JOptionPane.ERROR_MESSAGE
            );
        }
    }                                       

    public class Requestor{
        public MediaType MEDIA_TYPE_JPG  = MediaType.parse("image/jpg");
        public MediaType MEDIA_TYPE_APPLICATION  = MediaType.parse("application/octet-stream");
        public MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
        public OkHttpClient client = new OkHttpClient();
        
        String postFingerprintMultipart(String url, byte[] data, String personId, String fingerprintNumber) throws IOException {
            RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("personId", personId)
                .addFormDataPart("fingerprintNumber", fingerprintNumber)
                .addFormDataPart("fingerprint", "fingerprint", RequestBody.create(MEDIA_TYPE_APPLICATION, data))
                .build();
                
            Request request = new Request.Builder()
                    .header("Authorization", "Basic " + apiComposition)
                    .url(url) 
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                //System.out.println(response.body().string());
                return response.body().string();
            } 
        }
    }

    // Variables declaration - do not modify                     
    private javax.swing.JScrollPane JScrollPane1;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnRead;
    private javax.swing.JButton btnSave;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel jPanelBackground;
    private javax.swing.JPanel jPanelConsole;
    private javax.swing.JPanel jPanelLog;
    private javax.swing.JLabel lblConsole;
    private javax.swing.JLabel lblLog;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel picFingerprint;
    private javax.swing.JTextField txtConsole;
    private javax.swing.JTextArea txtLog;
    // End of variables declaration                   
}