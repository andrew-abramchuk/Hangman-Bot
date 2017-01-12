/*
package firstBot;

import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.sql.*;
import java.util.ArrayList;

public class BotDB {
    Connection dbConnection = null;
    boolean statusOfGame = false, riskGame = false;
    int errorCounter = 0, letterCounter = 0;
    String word = "", enteredLetters = "", wrongLetters = "";

    BotDB(String WORD, ){
       word = WORD;
    }

    private static Connection getDBConnection() {
        Connection dbCon = null;
        try {
            Class.forName("org.postgresql.Driver"); // Драйвер подключен
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbCon = DriverManager.getConnection( // Установление соединения
                    "jdbc:postgresql://localhost:5432/HangBotDB", "postgres", "591996f");
            return dbCon;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbCon;
    }

    private void addNewUser_DB(Message message, String helpText, String commandsText) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Long userChatId = message.getChatId();
        String selectTableSQL = "SELECT num_games FROM \"Statistics\" WHERE chat_id = " + userChatId + ";";
        ReplyKeyboardMarkup r = replyKbrd();
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            rs = preparedStatement.executeQuery();  // выбираем данные с БД
            if (!rs.next()) { // Если ничего не было получено, то добавляем пользователя в базу
                String insertTableSQL = String.format("INSERT INTO \"Statistics\"(" +
                        "chat_id, num_games, num_win, num_losing)\n" +
                        "VALUES(%d, %d, %d, %d);", userChatId, 0, 0, 0);
                preparedStatement = dbConnection.prepareStatement(insertTableSQL);
                preparedStatement.executeUpdate();
                sendMsg(message, helpText);
                sendImage(message, "G:\\Gallows\\welcome.jpg");
                sendMsgWithBtn(message, r, commandsText);
            } else
                sendMsgWithBtn(message, r, "\uD83D\uDCA1 Вы уже вели чат с Hangman Bot. Вся история и статистика сохранена.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("addNewUser_DB");
    }

    private void initNewGame_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        String insertTableSQL = String.format(
                "INSERT INTO \"CurrentGame\"(" +
                        "chat_id, \"StatusOfGame\", \"Word\", \"EnteredLetters\", \"LetterCounter\", \"ErrorCounter\", \"WrongLetters\", \"RiskGame\")" +
                        "VALUES (%d, %b, '%s', '%s', %d, %d, '%s', '%b' );", userChatId, statusOfGame, word, enteredLetters, letterCounter, errorCounter, wrongLetters, riskGame);
        try {
            preparedStatement = dbConnection.prepareStatement(insertTableSQL);
            preparedStatement.executeUpdate();  // добавление новой записи в БД
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("initNewGame_DB");
    }

    private void getStatusOfGame_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String selectTableSQL = "SELECT \"StatusOfGame\" FROM \"CurrentGame\" WHERE chat_id = " + userChatId + ";";
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            rs = preparedStatement.executeQuery();  // выбираем данные с БД
            if (!rs.next())  // Если ничего не было получено, то:
                statusOfGame = false;
            else
                statusOfGame = true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("getStatusOfGame_DB");
    }

    private void getCurGameInfo_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String selectTableSQL =
                "SELECT \"StatusOfGame\", \"Word\", \"EnteredLetters\", \"LetterCounter\", \"ErrorCounter\", \"WrongLetters\", \"RiskGame\" " +
                        "FROM \"CurrentGame\" WHERE chat_id = " + userChatId + ";";
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            rs = preparedStatement.executeQuery();  // выбираем данные с БД
            if (rs.next()) {
                statusOfGame = rs.getBoolean("StatusOfGame");
                word = rs.getString("Word");
                enteredLetters = rs.getString("EnteredLetters");
                letterCounter = rs.getInt("LetterCounter");
                errorCounter = rs.getInt("ErrorCounter");
                wrongLetters = rs.getString("WrongLetters");
                riskGame = rs.getBoolean("RiskGame");
            } else
                statusOfGame = false;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("getCurGameInfo_DB");
    }

    private void updateCurGameInfo_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        String updateTableSQL = String.format(
                "UPDATE \"CurrentGame\" " +
                        "SET \"StatusOfGame\"=%b, \"Word\"= '%s', \"EnteredLetters\"= '%s', \"LetterCounter\"=%d, \"ErrorCounter\"=%d, \"WrongLetters\" = '%s', \"RiskGame\" = '%b' " +
                        "WHERE chat_id = %d;", statusOfGame, word, enteredLetters, letterCounter, errorCounter, wrongLetters, riskGame, userChatId);
        try {
            preparedStatement = dbConnection.prepareStatement(updateTableSQL);
            preparedStatement.executeUpdate();  // обновление записи
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("updateCurGameInfo_DB");
    }

    private void deleteCurrentGame_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        String deleteTableSQL = "DELETE FROM \"CurrentGame\" WHERE chat_id = " + userChatId + ";";
        try {
            preparedStatement = dbConnection.prepareStatement(deleteTableSQL);
            preparedStatement.executeUpdate();  // удаляем запись из БД
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("deleteCurrentGame_DB");
    }

    private int getWordCheck_DB(Long userChatId, String selectedWord) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        int count = 0;
        ArrayList<String[]> wordsMas = new ArrayList<String[]>();
        String selectTableSQL = String.format("SELECT COUNT(*) FROM \"Words\" WHERE chat_id = '%d' AND word = '%s';", userChatId, selectedWord);
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            //statement = dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = preparedStatement.executeQuery();  // берем данные из БД
            while (rs.next())
                count = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("getWordCheck_DB, " + "COUNT = " + count);
        return count;
    }

    private int getCountTable_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        int count = 0;
        ArrayList<String[]> wordsMas = new ArrayList<String[]>();
        String selectTableSQL = "SELECT COUNT(*) FROM \"Words\";";
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            rs = preparedStatement.executeQuery();  // берем данные из БД
            while (rs.next())
                count = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("getCountTable_DB");
        return count;
    }

    private void addWord_DB(Long userChatId, String word) {
        PreparedStatement preparedStatement = null;
        int id = getCountTable_DB(userChatId) + 1;
        String insertTableSQL = String.format("INSERT INTO \"Words\"(" +
                "chat_id, word, id)" +
                "VALUES (%d, '%s', %d);", userChatId, word, id);
        try {
            preparedStatement = dbConnection.prepareStatement(insertTableSQL);
            preparedStatement.executeUpdate();  // добавление новой записи в БД
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("addWord_DB");
    }

    private int[] getStatistics_DB(Long userChatId) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        String selectTableSQL = "SELECT num_games, num_win, num_losing FROM \"Statistics\" WHERE chat_id = " + userChatId + ";";
        int[] statMas = new int[3];
        try {
            preparedStatement = dbConnection.prepareStatement(selectTableSQL);
            rs = preparedStatement.executeQuery();  // выбираем данные с БД
            while (rs.next()) {
                statMas[0] = rs.getInt("num_games");
                statMas[1] = rs.getInt("num_win");
                statMas[2] = rs.getInt("num_losing");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            try {
                preparedStatement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("getStatistics_DB");
        return statMas;
    }

    private void updateStatistics_DB(Long userChatId, boolean outcome) {
        Statement statement = null;
        int[] statMas = getStatistics_DB(userChatId);
        statMas[0] += 1;
        if (outcome == true) statMas[1] += 1;
        else statMas[2] += 1;
        String updateTableSQL = String.format("UPDATE \"Statistics\" " +
                "SET num_games = %d, num_win = %d, num_losing = %d WHERE chat_id = %d;", statMas[0], statMas[1], statMas[2], userChatId);
        try {
            statement = dbConnection.createStatement();
            statement.executeUpdate(updateTableSQL);  // обновление записи
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                statement.close();
            } catch (SQLException ignore) {
            }
        }
        System.out.println("updateStatistics_DB");
    }

}
*/
