package com.zhangke.mail;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.eagatech.richcat.test.RoundProgressDialog;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by 张可 on 2017/7/14.
 */

public class SendMailAsyncTask  extends AsyncTask<Integer, Integer, String> {

    private Activity activity;
    private RoundProgressDialog progressDialog;

    public SendMailAsyncTask(Activity activity) {
        this.activity = activity;
        this.progressDialog = new RoundProgressDialog(activity);
    }

    @Override
    protected String doInBackground(Integer... integers) {
        sendMail();
        return null;
    }
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        progressDialog.closeProgressDialog();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog.showProgressDialog();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    /**
     * @throws MessagingException
     */
    public static void sendMail() {
        final String sendUserName = "zhangke_711@163.com";
        final String sendPassword = "zk151018";

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.auth", "true");//服务器需要认证
        properties.setProperty("mail.smtp.host", "smtp.163.com");//声明发送邮件使用的端口

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sendUserName, sendPassword);
            }
        };
        Session session = Session.getInstance(properties, authenticator);
        session.setDebug(true);//同意在当前线程的控制台打印与服务器对话信息

        try {
            Message message = new MimeMessage(session);//构建发送的信息
            message.setText("你好，我是 Java mail！");//信息内容
            message.setFrom(new InternetAddress(sendUserName));//发件人
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("1282403981@qq.com"));
            message.saveChanges();
            Transport.send(message);
//            Transport transport = session.getTransport();
//            transport.connect("smtp.126.com", 25, sendUserName, sendPassword);//连接发件人使用发件的服务器
//            transport.sendMessage(message, new Address[]{new InternetAddress("zhangke_711@163.com")});//接受邮件
        }catch(MessagingException e){
            e.printStackTrace();
            Log.e("SendMail", e.getMessage());
        }
    }
}
