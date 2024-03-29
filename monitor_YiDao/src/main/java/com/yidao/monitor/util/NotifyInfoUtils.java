package com.yidao.monitor.util;
import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.yidao.monitor.util.PSFClient.PSFRPCRequestData;

public class NotifyInfoUtils {

	private MimeMessage message;
	private Session session;
	private Transport transport;
	private String mailHost = "";
	private String sender_username = "";
	private String sender_password = "";
	private Properties properties ;
    private PSFClient psfclient = null;
    private PSFClient.PSFRPCRequestData request = null;
	public static void main(String [] args){
		MailTest mail = new MailTest(true);
		mail.doSendHtmlEmail("测试邮件", "测试邮件内容", "wanghao@yongche.com"	, null);

	}
	public NotifyInfoUtils(boolean debug) {
		properties = new Properties();
        properties.put("mail.smtp.host", "smtp.yongche.com");
        properties.put("mail.smtp.auth","true");
//        properties.put("mail.sender.username", "wangbingwei@yongche.com");
//        properties.put("mail.sender.password","Password01!");
		properties.put("mail.sender.username", "wanghao@yongche.com");
		properties.put("mail.sender.password","w894080");
		this.mailHost = properties.getProperty("mail.smtp.host");
        this.sender_username = properties.getProperty("mail.sender.username");
        this.sender_password = properties.getProperty("mail.sender.password");
	    session = Session.getInstance(properties);
	    session.setDebug(debug);// 开启后有调试信息
	    message = new MimeMessage(session);
	}
	public boolean  doSendSMS(String phonesStr,String content){
		boolean sendStatus=true;
		 String [] phones = phonesStr.split(",");
		 
		  //线上环境地址
		      String[] serviceCenter = {"172.17.0.77:5201","172.17.0.78:5201"};
	      //测试环境地址
		      //   String[] serviceCenter = {"10.0.11.71:5201","10.0.11.72:5201"};
		  try{
			  psfclient = new PSFClient(serviceCenter);	
	      	  request = new PSFClient.PSFRPCRequestData(); 
	      	 request.service_uri="event/sendWarning";
			  for(int i=0;i<phones.length;i++){
	    	    	request.data="{\"CELLPHONE\": \""+phones[i]+"\",\"CONTENT\": \""+content+"\",\"FLAG\": 14,\"__NO_ASSEMBLE\": 1,\"__EVENT_ID__\": 46}";
	    	    	psfclient.call("atm2", request);	 
	    	     } 	  
		  }catch(Exception e){
			  sendStatus=false;
			  e.printStackTrace();
		  }finally{
			  psfclient.close();
		  }
    	 return sendStatus;   
	}

	/**
	 * 发送邮件
	 * 
	 * @param subject
	 *            邮件主题
	 * @param sendHtml
	 *            邮件内容
	 * @param receiveUser
	 *            收件人地址
	 * @param attachment
	 *            附件
	 */
	public boolean doSendHtmlEmail(String subject, String sendHtml, String receiveUser, File attachment) {
		boolean sendStatus = true;
	    try {
	        // 发件人
	        InternetAddress from = new InternetAddress(sender_username);
	        message.setFrom(from);

//	        收件人 当收件人只有一个的时候，调用setRecipient方法，同参数为InternetAddress
//	        InternetAddress to = new InternetAddress(receiveUser);
//	        message.setRecipient(Message.RecipientType.TO, to);
	        //收件人 当收件人多个的时候，调用setRecipient方法，同参数为InternetAddress
	        String [] receiveUsers = receiveUser.split(",");
	        InternetAddress[] toall = new InternetAddress[receiveUsers.length];
	        for(int i=0;i<toall.length;i++){
	        	toall[i]=new InternetAddress(receiveUsers[i]);
	        }
	        message.setRecipients(Message.RecipientType.TO,toall);
	        // 邮件主题
	        message.setSubject(subject);

	        // 向multipart对象中添加邮件的各个部分内容，包括文本内容和附件
	        Multipart multipart = new MimeMultipart();
	        
	        // 添加邮件正文
	        BodyPart contentPart = new MimeBodyPart();
	        contentPart.setContent(sendHtml, "text/html;charset=UTF-8");
	        multipart.addBodyPart(contentPart);
	        
	        // 添加附件的内容
	        if (attachment != null) {
	            BodyPart attachmentBodyPart = new MimeBodyPart();
	            DataSource source = new FileDataSource(attachment);
	            attachmentBodyPart.setDataHandler(new DataHandler(source));
	            
	            // 网上流传的解决文件名乱码的方法，其实用MimeUtility.encodeWord就可以很方便的搞定
	            // 这里很重要，通过下面的Base64编码的转换可以保证你的中文附件标题名在发送时不会变成乱码
	            //sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
	            //messageBodyPart.setFileName("=?GBK?B?" + enc.encode(attachment.getName().getBytes()) + "?=");
	            
	            //MimeUtility.encodeWord可以避免文件名乱码
	            attachmentBodyPart.setFileName(MimeUtility.encodeWord(attachment.getName()));
	            multipart.addBodyPart(attachmentBodyPart);
	        }
	        
	        // 将multipart对象放到message中
	        message.setContent(multipart);
	        // 保存邮件
	        message.saveChanges();

	        transport = session.getTransport("smtp");
	        // smtp验证，就是你用来发邮件的邮箱用户名密码
	        transport.connect(mailHost, sender_username, sender_password);
	        // 发送
	        transport.sendMessage(message, message.getAllRecipients());

	        System.out.println("send success!");
	    } catch (Exception e) {
	    	sendStatus=false;
	        e.printStackTrace();
	    } finally {
	        if (transport != null) {
	            try {
	                transport.close();
	            } catch (MessagingException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    return sendStatus;
	}
	
}
