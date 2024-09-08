package org.brownsolutions.service;

import org.brownsolutions.model.CTeResponseDTO;
import org.brownsolutions.utils.AlertPane;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

public class CTeStatusService {

    private final String certifiedServerPath;
    private final String certifiedPath;
    private final String password;
    private final String cTeKey;

    public CTeStatusService(String certifiedServerPath, String certifiedPath, String password, String cTeKey) {
        this.certifiedServerPath = certifiedServerPath;
        this.certifiedPath = certifiedPath;
        this.password = password;
        this.cTeKey = cTeKey;
    }

    public CTeResponseDTO consultEndpoint() {
        try {
            KeyStore keyStore = generateKeyStore();
            KeyManagerFactory kmf = generateKeyManagerFactory(keyStore);

            TrustManagerFactory tmf = getTrustManagerFactoryWithCert();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

            HttpsURLConnection connection = getConnection(sslContext);
            if (connection == null) return null;

            String soapRequest = getSoapRequest();
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = soapRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    return parseXMLResponse(content.toString());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private KeyStore generateKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("PKCS12");

            try (FileInputStream fis = new FileInputStream(certifiedPath)) {
                keyStore.load(fis, password.toCharArray());
            }

            return keyStore;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyManagerFactory generateKeyManagerFactory(KeyStore keyStore) {
        KeyManagerFactory kmf;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());
            return kmf;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TrustManagerFactory getTrustManagerFactoryWithCert() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            try (FileInputStream certInputStream = new FileInputStream(certifiedServerPath)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);
                trustStore.setCertificateEntry("cte-service", cert);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            return tmf;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getSoapRequest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                + "xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap12:Body>"
                + "<cteDadosMsg xmlns=\"http://www.portalfiscal.inf.br/cte/wsdl/CTeConsultaV4\">"
                + "<consSitCTe xmlns=\"http://www.portalfiscal.inf.br/cte\" versao=\"4.00\">"
                + "<tpAmb>1</tpAmb>"
                + "<xServ>CONSULTAR</xServ>"
                + "<chCTe>" + cTeKey + "</chCTe>"
                + "</consSitCTe>"
                + "</cteDadosMsg>"
                + "</soap12:Body>"
                + "</soap12:Envelope>";
    }

    private HttpsURLConnection getConnection(SSLContext sslContext) {
        try {
            URL url = new URI("https://cte.svrs.rs.gov.br/ws/CTeConsultaV4/CTeConsultaV4.asmx").toURL();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");

            return connection;

        } catch (Exception e) {
            AlertPane.showAlert("Erro!", "Serviço indisponível.");
            return null;
        }
    }

    private CTeResponseDTO parseXMLResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));

            NodeList cStatList = document.getElementsByTagName("cStat");
            NodeList xMotivoList = document.getElementsByTagName("xMotivo");

            if (cStatList.getLength() > 0 && xMotivoList.getLength() > 0) {
                String code = cStatList.item(0).getTextContent();
                if (Objects.equals(code, "100")) return null;

                String description = xMotivoList.item(0).getTextContent();

                return new CTeResponseDTO(code, description);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
