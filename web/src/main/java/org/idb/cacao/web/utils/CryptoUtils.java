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
package org.idb.cacao.web.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.Recipient;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Utility methods for cryptography
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CryptoUtils {

	private static final Pattern pCN = Pattern.compile("\\bCN=([^\\,]*)", Pattern.CASE_INSENSITIVE);

	/**
	 * Encrypts some text with the provided public key. Use the Cryptographic Message Syntax. Return the
	 * contents in Base64 format.
	 */
	public static String encrypt(String content, Key pubKey, X509Certificate  cert) throws Exception {
		byte[] contentBytes = content.getBytes();
		CMSProcessableByteArray cms = new CMSProcessableByteArray(contentBytes);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(buffer);
			encrypt(cms, out, cert);
			out.flush();
			out.close();
			out = null;
			byte[] cipherContent = buffer.toByteArray();
	    	String encoded = Base64.getEncoder().encodeToString(cipherContent);
	    	return encoded;
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	protected static void encrypt(CMSProcessable content, OutputStream out, X509Certificate  cert) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		int keysize = 128; // bits
		CMSEnvelopedDataStreamGenerator fact = new CMSEnvelopedDataStreamGenerator();
		fact.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert).setProvider("BC"));
		OutputStream cout = fact.open(out, new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC, keysize).setProvider("BC").build());
		content.write(cout);
		cout.flush();
		cout.close();
		return;
	}

	/**
	 * Decrypts come contents formatted in Base64 form.  Expect a message coded in Cryptographic Message Syntax. Make use of the
	 * provided private key.
	 */
	public static String decrypt(String cipherContent, PrivateKey privKey) throws Exception {
    	byte[] cipherContentBytes = Base64.getDecoder().decode(cipherContent.getBytes());

		InputStream cin = null;
		try {
			CMSEnvelopedDataParser envData = new CMSEnvelopedDataParser(cipherContentBytes);
			cin = decrypt(envData, privKey);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1000];
			int bytes_read;
			while ((bytes_read = cin.read(buffer)) > 0) {
				out.write(buffer, 0, bytes_read);
			}
			byte[] decryptedContent = out.toByteArray();
	    	String decoded = new String(decryptedContent);
	    	return decoded;
		} finally {
			if (cin != null) {
				cin.close();
				cin = null;
			}
		}
	}

	protected static InputStream decrypt(CMSEnvelopedDataParser envData, PrivateKey privKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		RecipientInformationStore recipients = envData.getRecipientInfos();
		@SuppressWarnings("unchecked")
		Iterator<RecipientInformation> iter = recipients.getRecipients().iterator();
		CMSTypedStream resultado = null;
		Exception last_exception = null;
		while ((iter.hasNext()) && (resultado == null)) {
			try {
				KeyTransRecipientInformation ktri = (KeyTransRecipientInformation) iter.next();
				Recipient recipient = new JceKeyTransEnvelopedRecipient(privKey).setProvider("BC");
				resultado = ktri.getContentStream(recipient);
			} catch (Exception e) {
				last_exception = e;
				resultado = null;
			}
		}

		if (resultado == null) {
			if (last_exception != null)
				throw new Exception("Falha na descriptografia!",last_exception);
			else
				throw new Exception("Falha na descriptografia!");
		}
		return resultado.getContentStream();
	}

	/**
	 * Given the principal field in X500 syntax, returns the common name.
	 */
	public static String getCommonName(X500Principal principal) {
		return getPart(principal, pCN);
	}
	
	/**
	 * Extracts part of the principal field in X500 syntax according to a regular expression
	 */
	private static String getPart(X500Principal principal, Pattern p) {
		String name = principal.getName();
		if (name == null)
			return null;
		Matcher m = p.matcher(name);
		if (m.find()) {
			String value = m.group(1).trim();
			if (value.length() == 0)
				return null;
			if (value.equalsIgnoreCase("Unknown"))
				return null;
			return value;
		}
		return null;
	}
	
}
