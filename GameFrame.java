/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package whowantstobeamillionaire;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import java.sql.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
/**
 *
 * @author nika2
 */
public class GameFrame extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GameFrame.class.getName());

    /**
     * Creates new form GameFrame
     */
    ArrayList<Question> questions = new ArrayList<Question>();
    private Random rnd = new Random();
    int Level = 0;
    Question currentQuestion;
    String playerName = "Игрок";
    String[] prizes = {"500","1 000","2 000","3 000","5 000","10 000","15 000",
        "25 000","50 000","100 000","200 000","400 000","800 000","1 500 000","3 000 000"};

    public GameFrame() {
        initComponents();
         ReadBase();
         CreateRecordsTable();
        startGame();
        setSize(770, 585);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void ReadBase() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(
                    "jdbc:sqlite:WhoWantsToBeAMillionaire.db");
            Statement statmt = conn.createStatement();

            String query = "select Text, Answer1, Answer2, Answer3, "
                    + "Answer4, RightAnswer, Level from Questions";
            ResultSet rs = statmt.executeQuery(query);

            while (rs.next()) {
                String[] s = new String[]{
                    rs.getString("Text"),
                    rs.getString("Answer1"),
                    rs.getString("Answer2"),
                    rs.getString("Answer3"),
                    rs.getString("Answer4"),
                    rs.getString("RightAnswer"),
                    String.valueOf(rs.getInt("Level"))
                };
                questions.add(new Question(s));
            }

            rs.close();
            conn.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private void ShowQuestion(Question q) {
        lblQuestionText.setText("<html><div style='text-align:center'>"
            + q.Text + "</div></html>");
        btnAnswer1.setText("<html>A. " + q.Answers[0] + "</html>");
        btnAnswer2.setText("<html>B. " + q.Answers[1] + "</html>");
        btnAnswer3.setText("<html>C. " + q.Answers[2] + "</html>");
        btnAnswer4.setText("<html>D. " + q.Answers[3] + "</html>");
    }

    private Question GetQuestion(int level) {
        List<Question> list =
                questions.stream().filter(q -> q.Level == level).collect(Collectors.toList());
        return list.get(rnd.nextInt(list.size()));
    }
    
    private Question GenerateQuestion(int level) {
        try {
            String apiKey = "ВАШ_API_КЛЮЧ";

            String slozhnost;
            if (level <= 5) slozhnost = "лёгкий, для широкой аудитории";
            else if (level <= 10) slozhnost = "средней сложности, требующий эрудиции";
            else slozhnost = "сложный, на узкие знания в науке, истории, искусстве";

            String prompt = "Придумай оригинальный вопрос для викторины "
                + "Кто хочет стать миллионером на русском языке. "
                + "Тема — любая (история, наука, культура, спорт, география). "
                + "Уровень: " + slozhnost + " (вопрос " + level + " из 15, сложность растёт). "
                + "+ \"Примеры стиля вопросов: \"\n"
                + "1) Как называется детская игрушка-неваляшка? \"\n"
                + "2) Что показывает судья футболисту, делая предупреждение? \"\n"
                + "Сделай вопрос в похожем стиле, но на свою тему. \""
                + "Сделай вопрос интересным и не банальным. "
                + "СТРОГО: вопрос не длиннее 12 слов. "
                + "Каждый вариант ответа — не длиннее 3 слов. "
                + "ВАЖНО: тщательно проверь, что поле right указывает на ДЕЙСТВИТЕЛЬНО правильный ответ. "
                + "Верни строго JSON без пояснений и без markdown: "
                + "{\"text\":\"вопрос\",\"answers\":[\"a\",\"b\",\"c\",\"d\"],"
                + "\"right\":\"номер правильного ответа от 1 до 4\"}";

            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("model", "openai/gpt-4o");
            reqBody.addProperty("temperature", 0.9);
            reqBody.add("messages", messages);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://polza.ai/api/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

            content = content.replace("```json", "").replace("```", "").trim();

            JsonObject q = JsonParser.parseString(content).getAsJsonObject();
            JsonArray ans = q.getAsJsonArray("answers");
            String[] s = new String[]{
                q.get("text").getAsString(),
                ans.get(0).getAsString(),
                ans.get(1).getAsString(),
                ans.get(2).getAsString(),
                ans.get(3).getAsString(),
                q.get("right").getAsString(),
                String.valueOf(level)
            };
            return new Question(s);

        } catch (Exception ex) {
            System.out.println("Ошибка генерации: " + ex.toString());
            return GetQuestion(level);
        }
    }

    private void NextStep() {
        JButton[] btns = new JButton[]{btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4};
        for (JButton btn : btns)
            btn.setEnabled(true);
        if (Level == 0) {
            btnFiftyFifty.setEnabled(true);
            btnHall.setEnabled(true);
            btnFriend.setEnabled(true);
        }
        Level++;
        currentQuestion = GenerateQuestion(Level);
        // currentQuestion = GetQuestion(Level);
        ShowQuestion(currentQuestion);
        lstLevel.setSelectedIndex(lstLevel.getModel().getSize() - Level);
    }

    private void startGame() {
         playerName = JOptionPane.showInputDialog(this, "Введите ваше имя:");
        if (playerName == null || playerName.isEmpty())
            playerName = "Игрок";
        Level = 0;
        NextStep();
    }

    private void CreateRecordsTable() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(
                    "jdbc:sqlite:WhoWantsToBeAMillionaire.db");
            Statement st = conn.createStatement();
            st.executeUpdate("create table if not exists Records ("
                    + "Name text, Score integer)");
            conn.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private void SaveRecord(int score) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(
                    "jdbc:sqlite:WhoWantsToBeAMillionaire.db");
            PreparedStatement ps = conn.prepareStatement(
                    "insert into Records (Name, Score) values (?, ?)");
            ps.setString(1, playerName);
            ps.setInt(2, score);
            ps.executeUpdate();
            conn.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private void ShowTopRecords() {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(
                    "jdbc:sqlite:WhoWantsToBeAMillionaire.db");
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                    "select Name, Score from Records order by Score desc limit 10");

            StringBuilder sb = new StringBuilder("Таблица рекордов (TOP-10):\n\n");
            int place = 1;
            while (rs.next()) {
                sb.append(place).append(". ")
                  .append(rs.getString("Name")).append(" — ")
                  .append(rs.getInt("Score")).append(" руб.\n");
                place++;
            }

            rs.close();
            conn.close();
            JOptionPane.showMessageDialog(this, sb.toString());
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
    
    private int CurrentPrize() {
        if (Level <= 1) return 0;
        String s = prizes[Level - 2].replace(" ", "");
        return Integer.parseInt(s);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnFiftyFifty = new javax.swing.JButton();
        btnHall = new javax.swing.JButton();
        btnFriend = new javax.swing.JButton();
        btnTakeMoney = new javax.swing.JButton();
        lblQuestionText = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstLevel = new javax.swing.JList<>();
        btnAnswer1 = new javax.swing.JButton();
        btnAnswer2 = new javax.swing.JButton();
        btnAnswer3 = new javax.swing.JButton();
        btnAnswer4 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnFiftyFifty.setText(" 50/50");
        btnFiftyFifty.addActionListener(this::btnFiftyFiftyActionPerformed);
        getContentPane().add(btnFiftyFifty, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 15, 120, -1));

        btnHall.setText("Помощь зала");
        btnHall.addActionListener(this::btnHallActionPerformed);
        getContentPane().add(btnHall, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 44, 120, -1));

        btnFriend.setText("Звонок другу");
        btnFriend.addActionListener(this::btnFriendActionPerformed);
        getContentPane().add(btnFriend, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 73, 120, -1));

        btnTakeMoney.setText(" Забрать деньги");
        btnTakeMoney.addActionListener(this::btnTakeMoneyActionPerformed);
        getContentPane().add(btnTakeMoney, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 124, -1, -1));

        lblQuestionText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblQuestionText.setText("jLabel1");
        getContentPane().add(lblQuestionText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 360, 710, 47));

        lstLevel.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "3 000 000", "1 500 000", "800 000", "400 000", "200 000", "100 000", "50 000", "25 000", "15 000", "10 000", "5 000", "3 000", "2 000", "1 000", "500" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(lstLevel);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(622, 15, 88, 310));

        btnAnswer1.setText("jButton5");
        btnAnswer1.setActionCommand("1");
        btnAnswer1.addActionListener(this::btnAnswer1ActionPerformed);
        getContentPane().add(btnAnswer1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 420, 214, 29));

        btnAnswer2.setText("jButton6");
        btnAnswer2.setActionCommand("2");
        btnAnswer2.addActionListener(this::btnAnswer2ActionPerformed);
        getContentPane().add(btnAnswer2, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 420, 214, 29));

        btnAnswer3.setText("jButton7");
        btnAnswer3.setActionCommand("3");
        btnAnswer3.addActionListener(this::btnAnswer3ActionPerformed);
        getContentPane().add(btnAnswer3, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 467, 214, 30));

        btnAnswer4.setText("jButton8");
        btnAnswer4.setActionCommand("4");
        btnAnswer4.addActionListener(this::btnAnswer4ActionPerformed);
        getContentPane().add(btnAnswer4, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 470, 214, 29));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/whowantstobeamillionaire/picture.jpg"))); // NOI18N
        jLabel2.setText("jLabel2");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(144, 73, 449, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAnswer1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnswer1ActionPerformed
        if (currentQuestion.RightAnswer.equals(evt.getActionCommand()))
            NextStep();
        else {
            JOptionPane.showMessageDialog(this, "Неверный ответ!");
            SaveRecord(CurrentPrize());
            ShowTopRecords();
            startGame();
        }
    }//GEN-LAST:event_btnAnswer1ActionPerformed

    private void btnAnswer2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnswer2ActionPerformed
        if (currentQuestion.RightAnswer.equals(evt.getActionCommand()))
            NextStep();
        else {
            JOptionPane.showMessageDialog(this, "Неверный ответ!");
            SaveRecord(CurrentPrize());
            ShowTopRecords();
            startGame();
        }
    }//GEN-LAST:event_btnAnswer2ActionPerformed

    private void btnAnswer3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnswer3ActionPerformed
        if (currentQuestion.RightAnswer.equals(evt.getActionCommand()))
            NextStep();
        else {
            JOptionPane.showMessageDialog(this, "Неверный ответ!");
            SaveRecord(CurrentPrize());
            ShowTopRecords();
            startGame();
        }
    }//GEN-LAST:event_btnAnswer3ActionPerformed

    private void btnAnswer4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnswer4ActionPerformed
        if (currentQuestion.RightAnswer.equals(evt.getActionCommand()))
            NextStep();
        else {
            JOptionPane.showMessageDialog(this, "Неверный ответ!");
            SaveRecord(CurrentPrize());
            ShowTopRecords();
            startGame();
        }
    }//GEN-LAST:event_btnAnswer4ActionPerformed

    private void btnFiftyFiftyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFiftyFiftyActionPerformed
        JButton[] btns = new JButton[]{btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4};
        int count = 0;
        while (count < 2) {
            int n = rnd.nextInt(4);
            String ac = btns[n].getActionCommand();
            if (!ac.equals(currentQuestion.RightAnswer) && btns[n].isEnabled()) {
                btns[n].setEnabled(false);
                count++;
            }
        }
        btnFiftyFifty.setEnabled(false);
    }//GEN-LAST:event_btnFiftyFiftyActionPerformed

    private void btnHallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHallActionPerformed
        int right = Integer.parseInt(currentQuestion.RightAnswer);
        int[] proc = new int[4];
        int correct = 40 + rnd.nextInt(41);
        proc[right - 1] = correct;
        int left = 100 - correct;
        for (int i = 0; i < 4; i++) {
            if (i != right - 1) {
                int p = (i < 3) ? rnd.nextInt(left + 1) : left;
                proc[i] = p;
                left -= p;
            }
        }
        String txt = "Результаты голосования зала:\n"
            + "A: " + proc[0] + "%\n"
            + "B: " + proc[1] + "%\n"
            + "C: " + proc[2] + "%\n"
            + "D: " + proc[3] + "%";
        JOptionPane.showMessageDialog(this, txt);
        btnHall.setEnabled(false);
    }//GEN-LAST:event_btnHallActionPerformed

    private void btnFriendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFriendActionPerformed
        int right = Integer.parseInt(currentQuestion.RightAnswer);
        String[] letters = {"A", "B", "C", "D"};
        String guess;
        if (rnd.nextInt(100) < 70) {
            guess = letters[right - 1];
        } else {
            guess = letters[rnd.nextInt(4)];
        }
        JOptionPane.showMessageDialog(this,
            "Друг отвечает: думаю, правильный ответ — вариант " + guess);
        btnFriend.setEnabled(false);
    }//GEN-LAST:event_btnFriendActionPerformed

    private void btnTakeMoneyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTakeMoneyActionPerformed
       int prize = CurrentPrize();
        JOptionPane.showMessageDialog(this, "Вы забрали выигрыш: " + prize + " руб.");
        SaveRecord(prize);
        ShowTopRecords();
        startGame();
    }//GEN-LAST:event_btnTakeMoneyActionPerformed

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
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new GameFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAnswer1;
    private javax.swing.JButton btnAnswer2;
    private javax.swing.JButton btnAnswer3;
    private javax.swing.JButton btnAnswer4;
    private javax.swing.JButton btnFiftyFifty;
    private javax.swing.JButton btnFriend;
    private javax.swing.JButton btnHall;
    private javax.swing.JButton btnTakeMoney;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblQuestionText;
    private javax.swing.JList<String> lstLevel;
    // End of variables declaration//GEN-END:variables
}
