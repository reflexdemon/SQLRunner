package org.reflexdemon.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.mail.smtp.SMTPMessage;

public class SendEmail {
	// Cbeyond Billing<billing@cbeyond.net>
	// No Reply<noreply@cbeyond.net>
	private static final String FROM_FIELD = "No Reply<noreply@cbeyond.net>";
	private static final String SMTP_SERVER = "emr.cbeyond.net";
	private static Session mailSession;
	private static Log log = LogFactory.getLog(SendEmail.class);

	public static void main(String args[]) {
		SendEmail mailer = new SendEmail();
		String sendHTML = "<html><body><h1>Test Mail</h1><p>This is a Test message. This is just to demo how the mail would look like for the customer. <br/><br/>If you have any questions please mail to IBILL@cbeyond.net.<br/><br/>Regards,<br/>Venkateswara VP</p></body></html>";
		String sendText = "Sample text message";
		String[] emailAddrs = { "venkateswara.venkatraman@cbeyond.net", "",
				"venkateswara.venkatraman@cbeyond.net" };
		// String[] emailAddrs = { "Jennifer.Pendleton@cbeyond.net",
		// "venkateswara.venkatraman@cbeyond.net",
		// "Aspen.Kron@cbeyond.net" };

		String subject = "V_RESOURCE SQL Log";
		mailer.send(sendHTML, sendText, emailAddrs, subject);
	}

	public SendEmail() {
	}

	protected static void getJavaMailSession(String smtpHost) {
		try {
			if (mailSession == null) {
				Properties props = System.getProperties();
				if (smtpHost != null) {
					props.put("mail.smtp.host", smtpHost);
				}
				mailSession = Session.getDefaultInstance(props, null);
				mailSession.setDebug(false);
				log.debug("Getting New JavaMailSession");
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}

	}

	public static boolean send(String sendHTML, String sendText,
			String[] emailAddrs, String subject) {
		boolean sendFlag = false;
		try {
			sendMail(sendHTML, sendText, emailAddrs, subject);
			sendFlag = true;
		} catch (MessagingException me) {
			me.printStackTrace();
			try {
				sendMail(sendHTML, sendText, emailAddrs, subject);
				sendFlag = true;
			} catch (Exception ex) {
				sendFlag = false;
				ex.printStackTrace();
			}
		} catch (Exception e) {
			sendFlag = false;
			e.printStackTrace();
		}
		return sendFlag;
	}

	public static void sendMail(String htmlMessage, String textMessage,
			String[] emailAddrs, String subject) throws MessagingException {
		sendMessage(emailAddrs, subject, textMessage, htmlMessage);

	}

	public static void sendEmail(String message, String[] emailAddrs, String sub) {
		try {
			sendMessage(emailAddrs, sub, message, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendMessage(String[] emailAddrs, String subject,
			String textMessage, String htmlMessage) throws MessagingException {
		getJavaMailSession(SMTP_SERVER);
		SMTPMessage msg = new SMTPMessage(mailSession);

		msg.setFrom(new InternetAddress(FROM_FIELD));
		msg.setSubject(subject);

		for (int i = 0; i < emailAddrs.length; i++) {
			if (!(emailAddrs[i] == null || emailAddrs[i].length() <= 4)) {
				InternetAddress address[] = { new InternetAddress(emailAddrs[i]) };
				if (i == 0) {
					msg.setRecipients(javax.mail.Message.RecipientType.TO,
							address);
					log.info("Email sent to " + emailAddrs[i] + " for account ");
				} else if (i == 1) {
					msg.setRecipients(javax.mail.Message.RecipientType.BCC,
							address);
				} else if (i == 2) {
					msg.setRecipients(javax.mail.Message.RecipientType.CC,
							address);
				}
			}
		}
		BodyPart body = new MimeBodyPart();
		body.setText(textMessage);
		MimeMultipart multipart = null;
		if (htmlMessage != null) {
			BodyPart html = new MimeBodyPart();
			html.setContent(htmlMessage, "text/html");
			multipart = new MimeMultipart("alternative");
			multipart.addBodyPart(body);
			multipart.addBodyPart(html);
		} else {
			body.setHeader("X-Mailer", "sendMail");
			body.setDataHandler(new DataHandler(new ByteArrayDataSource(
					textMessage, "text/html")));
			multipart = new MimeMultipart();
			multipart.addBodyPart(body);
		}
		msg.setContent(multipart);
		Calendar time = Calendar.getInstance();
		// time.add(Calendar.MONTH, -1);
		msg.setSentDate(time.getTime());
		Transport.send(msg);
	}

	public static String readTemplate(String templateLocation) {
		String returnvalue = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(
					templateLocation));

			String str;
			while ((str = in.readLine()) != null) {
				returnvalue += str;
			}
			in.close();
		} catch (java.io.IOException e) {

			throw new RuntimeException("File not found ", e);
		}
		return returnvalue;
	}

}
