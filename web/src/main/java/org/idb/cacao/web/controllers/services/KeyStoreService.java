/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.idb.cacao.web.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Basic functionality with local internal application keystore
 * 
 * @author Gustavo Figueiredo
 *
 */
@Service
public class KeyStoreService {

	private static final Logger log = Logger.getLogger(KeyStoreService.class.getName());

	private KeyStore keystore;

	@Autowired
	private Environment env;

	public KeyStore getKeyStore() {
		if (keystore!=null)
			return keystore;
		String keystore_file = env.getProperty("mail.ssl.key-store");
		if (keystore_file==null || keystore_file.trim().length()==0)
			return null;
		String keystore_password = env.getProperty("mail.ssl.key-store-password");
		if (keystore_password==null)
			keystore_password = "";
		if (keystore_file.startsWith("classpath:")) {
			String keystore_resource_name = keystore_file.substring("classpath:".length());
			if (!keystore_resource_name.startsWith("/"))
				keystore_resource_name = "/"+keystore_resource_name;
			URL url = KeyStoreService.class.getResource(keystore_resource_name);
			if (url==null) {
				log.log(Level.SEVERE, "Could not find resource informed in 'server.ssl.key-store' application property: "+keystore_file);
				return null;
			}
			try {
				KeyStore keystore = KeyStore.getInstance("pkcs12", "SunJSSE");
				keystore.load(url.openStream(), keystore_password.toCharArray());
				this.keystore = keystore;
				return keystore;
			}
			catch (CertificateException | KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | IOException ex) {
				log.log(Level.SEVERE, "Could not load resource informed in 'server.ssl.key-store' application property: "+keystore_file, ex);
				return null;				
			}
		}
		else if (keystore_file.startsWith("file:")) {
			String keystore_file_name = keystore_file.substring("file:".length());
			File file = new File(keystore_file_name);
			try (FileInputStream stream = new FileInputStream(file)) {
				KeyStore keystore = KeyStore.getInstance("pkcs12", "SunJSSE");
				keystore.load(stream, keystore_password.toCharArray());
				this.keystore = keystore;
				return keystore;
			}
			catch (CertificateException | KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | IOException ex) {
				log.log(Level.SEVERE, "Could not load resource informed in 'server.ssl.key-store' application property: "+keystore_file, ex);
				return null;				
			}
		}
		else {
			log.log(Level.SEVERE, "Unparseable value for 'server.ssl.key-store' application property: "+keystore_file);
			return null;
		}
	}
	
	public X509Certificate getCertificate() {
		KeyStore ks = getKeyStore();
		if (ks==null)
			return null;
		try {
			String keystore_password = env.getProperty("mail.ssl.key-store-password");
			if (keystore_password==null)
				keystore_password = "";
			for (String alias:Collections.list(ks.aliases())) {
				if (!ks.isKeyEntry(alias)) 
					continue;
				return (X509Certificate)ks.getCertificate(alias);
			}
		} catch (KeyStoreException e) {
			return null;
		}
		return null;
	}

	public PublicKey getPublicKey() {
		KeyStore ks = getKeyStore();
		if (ks==null)
			return null;
		try {
			String keystore_password = env.getProperty("mail.ssl.key-store-password");
			if (keystore_password==null)
				keystore_password = "";
			for (String alias:Collections.list(ks.aliases())) {
				if (!ks.isKeyEntry(alias)) 
					continue;
				return ks.getCertificate(alias).getPublicKey();
			}
		} catch (KeyStoreException e) {
			return null;
		}
		return null;
	}
	
	PrivateKey getPrivateKey() {
		KeyStore ks = getKeyStore();
		if (ks==null)
			return null;
		try {
			String keystore_password = env.getProperty("server.ssl.key-store-password");
			if (keystore_password==null)
				keystore_password = "";
			for (String alias:Collections.list(ks.aliases())) {
				if (!ks.isKeyEntry(alias)) 
					continue;
				return (PrivateKey)ks.getKey(alias, keystore_password.toCharArray());
			}
		} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
			return null;
		}	
		return null;
	}

	public String decrypt(String token) {
		if (token==null || token.length()==0)
			return token;
		PrivateKey key = getPrivateKey();
		if (key==null)
			return token;
		try {
			return CryptoUtils.decrypt(token, key);
		} catch (Exception e) {
			return token;
		}
	}
	
	public String encrypt(String token) {
		if (token==null || token.length()==0)
			return token;
		PrivateKey key = getPrivateKey();
		if (key==null)
			return token;
		try {
			return CryptoUtils.encrypt(token, key, getCertificate());
		} catch (Exception e) {
			return token;
		}
	}

	/**
	 * Verifies existence of the KeyStore to be used for SSL. Creates a self-signed
	 * certificate if the provided filename does not exist.
	 */
	public void assertKeyStoreForSSL() {
		try {
			
			if (!"true".equalsIgnoreCase(env.getProperty("server.ssl.enabled")))
				return;

			String keystore_prop = env.getProperty("server.ssl.key-store");
			if (keystore_prop==null || keystore_prop.trim().length()==0)
				return;
			
			if (!keystore_prop.startsWith("file:"))
				return;
			
			URL url = new URL(keystore_prop.replace("~", System.getProperty("user.home")));
			File file = FileUtils.toFile(url).getCanonicalFile();
			if (!file.exists()) {
				log.log(Level.WARNING, "Did not find keystore file "+file.getAbsolutePath()+", so will generate a self-signed!");
				String alias = env.getProperty("server.ssl.key-alias", "cacao");
				String password = env.getProperty("server.ssl.key-store-password");
				genSelfSigned(file, alias, password);
			}
		}
		catch (Throwable ex) {
			log.log(Level.SEVERE, "Error during initialization", ex);
		}
	}
	
	/**
	 * Generates self-signed certificate
	 */
	public static void genSelfSigned(File file, String aliasKeyEntry, String certificateKeyPassword) throws Exception {
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		
		final String domainName = "cacao.localhost";
		final int keySize = 2048;
		final int days_validity = 3650;
		final String signatureAlgorithm = "sha512WithRSA";
		final X500Name subjectDN = new X500Name("CN=" + domainName + ", OU=None, O=None L=None, C=None");
		final BigInteger serialNumber = BigInteger.valueOf(Math.abs(new SecureRandom().nextInt()));
		final Date validityStartDate = new Date(System.currentTimeMillis());
		final Date validityEndDate = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * days_validity));
		
		Security.addProvider(new BouncyCastleProvider());
		
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
		keyPairGenerator.initialize(keySize, new SecureRandom());
		
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
		X509v1CertificateBuilder builder = new X509v1CertificateBuilder(
			       subjectDN, 
			       serialNumber, 
			       validityStartDate,
			       validityEndDate, 
			       subjectDN, 
			       subPubKeyInfo);	
		
		final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider("BC").build(keyPair.getPrivate());
		final X509CertificateHolder certHolder = builder.build(signer);
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
		
		KeyStore keystore = KeyStore.getInstance("pkcs12", "SunJSSE");
		keystore.load(null);

		keystore.setKeyEntry(aliasKeyEntry, keyPair.getPrivate(), certificateKeyPassword.toCharArray(), new Certificate[] {cert});
		
		try (FileOutputStream fos = new FileOutputStream(file);) {
			keystore.store(fos, certificateKeyPassword.toCharArray());
		}		
	}
}
