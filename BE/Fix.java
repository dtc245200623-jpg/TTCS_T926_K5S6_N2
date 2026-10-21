import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;

public class Fix {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get("FE/dashboard.html");
        List<String> lines = Files.readAllLines(p);
        List<String> out = new ArrayList<>();
        
        int startChatbotHtml = -1;
        int endChatbotHtml = -1;
        
        for (int i=0; i<lines.size(); i++) {
            if (lines.get(i).contains("<!-- Chatbot Widget -->")) startChatbotHtml = i;
            if (lines.get(i).contains("<script>") && startChatbotHtml != -1 && endChatbotHtml == -1) endChatbotHtml = i;
        }
        
        for (int i=0; i<lines.size(); i++) {
            if (i >= startChatbotHtml && i < endChatbotHtml) {
                // skip
            } else {
                out.add(lines.get(i));
            }
        }
        
        Files.write(p, out);
        System.out.println("Removed HTML!");
        
        // Now remove CSS
        lines = Files.readAllLines(p);
        out = new ArrayList<>();
        int startCss = -1;
        int endCss = -1;
        for (int i=0; i<lines.size(); i++) {
            if (lines.get(i).contains("/* Chatbot UI */")) startCss = i;
            if (lines.get(i).contains("</style>") && startCss != -1 && endCss == -1) endCss = i;
        }
        
        for (int i=0; i<lines.size(); i++) {
            if (i >= startCss && i < endCss) {
                // skip
            } else {
                out.add(lines.get(i));
            }
        }
        Files.write(p, out);
        System.out.println("Removed CSS!");
    }
}
