
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Movies{


    private static final String IMAGE_PATH = "C:\\Users\\abc\\Desktop\\movies\\images\\";
    public static void main(String[] args){
        System.out.println("downloading...");
        Movies movies = new Movies();
        movies.getMovieModel();
    }

    private int start = 0;

    private void getMovieModel(){
        String url = String.format("https://movie.douban.com/people/127344843/collect?start=%s&sort=time&rating=all&filter=all&mode=grid", start);
        start += 15;
        String response = getRequest(url);

        final Pattern movieModelPattern = Pattern.compile("<img alt=(.{1,50}) src=(.{30,100}) class=\"\">");
        Matcher movieModelMatcher = movieModelPattern.matcher(response);
        Pattern imgUrlPattern = Pattern.compile("https://img(.{0,5}).doubanio.com/view/photo/s_ratio_poster/public/(.{1,30}).jpg");
        Pattern namePattern = Pattern.compile("alt=\"(.{1,20})\" ");
        boolean isEmpty = true;
        while(movieModelMatcher.find()){
            isEmpty = false;
            String filePath = "";
            String fileUrl = "";

            String modelString = movieModelMatcher.group();
            Matcher nameMatcher = namePattern.matcher(modelString);
            while(nameMatcher.find()){
                String movieName = nameMatcher.group();
                if(movieName != null && !movieName.isEmpty()) {
                    movieName = movieName.trim();
                    movieName = movieName.replaceAll(" ", "_");
                    movieName = movieName.replaceAll("\"", "");
                    movieName = movieName.replaceAll("alt=", "");
                    System.out.println(movieName);
                    filePath = String.format("%s%s.jpg", IMAGE_PATH, movieName);
                }
            }
            Matcher urlMatcher = imgUrlPattern.matcher(modelString);
            while(urlMatcher.find()){
                fileUrl = urlMatcher.group();
                System.out.println(fileUrl);
            }
            if(fileUrl != null && !fileUrl.isEmpty() && filePath != null && !filePath.isEmpty())
                downloadImage(fileUrl,filePath);
        }
        
        if(!isEmpty){
            getMovieModel();
        }
    }

    public String getRequest(String requestUrl) {
        String response = "";
        try {
            URL uri = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Content-Type", "text/html; charset=utf-8");
            final int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream in = conn.getInputStream();
                InputStreamReader isReader = new InputStreamReader(in);
                BufferedReader bufReader = new BufferedReader(isReader);
                String line;
                StringBuilder sbResponse=new StringBuilder();
                while ((line = bufReader.readLine()) != null) {
                    sbResponse.append(line);
                }
                response = sbResponse.toString();
            } else {
                System.out.println("internet error");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return response;
    }

    private void saveResponseToFile(String response){
        File outputFile = new File("C:\\Users\\abc\\Desktop\\movies\\outputFile.html");
        try {
            if (!outputFile.exists()) {
                System.out.println("create file:" + outputFile.getPath());
                outputFile.createNewFile();
            }
            PrintStream ps = new PrintStream(new FileOutputStream(outputFile));
            ps.println(response);
            System.out.println("file saved");
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void downloadImage(String imgUrl, String filePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File f = new File(filePath);
                if (f.exists()) {
                    f.delete();
                }
                InputStream is = null;
                OutputStream os = null;
                try {
                    URL url = new URL(imgUrl);
                    URLConnection con = url.openConnection();
                    is = con.getInputStream();
                    byte[] bs = new byte[1024];
                    int len;
                    os = new FileOutputStream(filePath);
                    while ((len = is.read(bs)) != -1) {
                        os.write(bs, 0, len);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        os.close();
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}