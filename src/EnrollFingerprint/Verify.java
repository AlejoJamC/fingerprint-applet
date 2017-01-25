/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package EnrollFingerprint;

import Enrollment.MainForm;
import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.*;
import com.digitalpersona.onetouch.capture.event.*;
import com.digitalpersona.onetouch.processing.*;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author AlejoDesktop
 */
public class Verify extends javax.swing.JFrame {
    
    private String apiComposition = "YWRtaW46MTIzNDU=";
    public String jsonres = "vacio";
    // Connection string format = jdbc:oracle:<drivertype>:<user>/<password>@<database>
    public static String connectionString = "jdbc:oracle:thin:analytics/qwerty@172.28.128.4:1521/XE";
    public static String TEMPLATE_PROPERTY = "template";
    private DPFPTemplate template;
    private DPFPTemplate templatefp1;
    private DPFPTemplate templatefp2;
    private DPFPCapture capturer = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment enroller = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificator = DPFPGlobal.getVerificationFactory().createVerification();

    /**
     * Creates new form Enroll
     */
    public Verify() {
        initComponents();
        //updateStatus();
        
       // Event listener actived when fingerprint template is ready
       this.addPropertyChangeListener(TEMPLATE_PROPERTY, new PropertyChangeListener(){
           public void propertyChange(PropertyChangeEvent evt){
               if(evt.getNewValue() == evt.getOldValue()){
                   return;
               }
               if(template != null){
                   JOptionPane.showMessageDialog(Verify.this,
                           "La huella capturada esta lista para ser guardada.",
                           "Captura y Registro de huellas",
                           JOptionPane.INFORMATION_MESSAGE
                   );
               }
           }
       });
    }
    
    protected void init(){
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
        if (features != null)
        {
                // Compare the feature set with our template
                DPFPVerificationResult result = verificator.verify(features, templatefp1);
                updateStatus(result.getFalseAcceptRate());
                if (result.isVerified())
                        makeReport("The fingerprint was VERIFIED.");
                else
                        makeReport("The fingerprint was NOT VERIFIED.");
        }
        

    }
    
    protected void start(){
        capturer.startCapture();
        setPrompt("Scanner de huellas listo a usarse, por favor introduzca la huella.");
    }
    
    protected void stop(){
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
    
    private void updateStatus(int FAR){
        setStatus(String.format("Calidad de la muestra: %1$s", FAR));
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
    
    public void testOracle(){
        Connection conn = null;
        try {
            //DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(connectionString);
            if(conn != null){
                System.out.println("Conexion con Oracle exitosa!.");
            }else{
                System.out.println("Error conectandoce a Oracle Local");
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanelBackground = new javax.swing.JPanel();
        picFingerprint = new javax.swing.JLabel();
        jPanelConsole = new javax.swing.JPanel();
        txtConsole = new javax.swing.JTextField();
        lblConsole = new javax.swing.JLabel();
        btnCancel = new javax.swing.JButton();
        jPanelLog = new javax.swing.JPanel();
        JScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        lblLog = new javax.swing.JLabel();
        btnVerify = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();
        btnFind = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Almacenar Huellas");
        setResizable(false);

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

        btnVerify.setText("Verificar Huella");
        btnVerify.setEnabled(false);
        btnVerify.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerifyActionPerformed(evt);
            }
        });

        lblStatus.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblStatus.setText("Intentos");

        btnFind.setText("Cargar Huellas");
        btnFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFindActionPerformed(evt);
            }
        });

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
                        .addComponent(btnFind)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnVerify)
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
                    .addComponent(btnVerify)
                    .addComponent(lblStatus)
                    .addComponent(btnFind))
                .addContainerGap())
        );

        btnFind.getAccessibleContext().setAccessibleName("Cargar Huellas");

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

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnVerifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerifyActionPerformed
        init();
        //updateStatus();
        start();
        //testOracle();
    }//GEN-LAST:event_btnVerifyActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        System.exit(0);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFindActionPerformed
        // Load the fingerprints for the current user
        JSONParser jsonparser = new JSONParser();
        try {
            Requestor requestor = new Requestor();
            //System.out.println(jsonres);
            jsonres = requestor.getFingerprints("http://localhost:3012/api/v1/fingerprints?personId=1");
            //System.out.println(jsonres);
            // JSON parser to the response
            //Object obj = jsonparser.parse(jsonres);
            //System.out.println(obj);
            System.out.println("the values of data element:");
            JSONObject json = (JSONObject) jsonparser.parse(jsonres);
            Object data = json.get("data");
            System.out.println(data);
            
            String strdata = data.toString();
            
            JSONObject jsondata = (JSONObject) jsonparser.parse(strdata);
            Object fp1 = jsondata.get("fingerprint1");
            System.out.println(fp1);
            
            Object fp2 = jsondata.get("fingerprint2");
            System.out.println(fp2);
            
            // Set DPFPTemplate with the values
            if(fp1 != null){
                templatefp1 = DPFPGlobal.getTemplateFactory().createTemplate();
                templatefp1.deserialize(fp1.toString().getBytes());
            }else{
                templatefp2 = null;
            }
            
            if(fp2 != null){
                templatefp2 = DPFPGlobal.getTemplateFactory().createTemplate();
                templatefp2.deserialize(fp2.toString().getBytes());
            }else{
                templatefp2 = null;
            }
            
            btnVerify.setEnabled(true);
            btnFind.setEnabled(false);
            setPrompt("Huellas cargadas en memoria. Listas para ser verificadas.");
            makeReport("Huellas listas para ser verificadas.");
        } catch (Exception e) {
            System.out.println(e);
        }
    }//GEN-LAST:event_btnFindActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Verify.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Verify.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Verify.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Verify.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Verify().setVisible(true);
            }
        });
    }
    
    public class Requestor{
        public OkHttpClient client = new OkHttpClient();
        
        String getFingerprints(String url) throws IOException{
            Request request = new Request.Builder()
                    .header("Authorization", "Basic " + apiComposition)
                    .url(url) 
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } 
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane JScrollPane1;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnFind;
    private javax.swing.JButton btnVerify;
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
    // End of variables declaration//GEN-END:variables
}
