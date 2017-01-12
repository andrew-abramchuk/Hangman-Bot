package firstBot;

import java.io.*;
import java.sql.*;
import java.util.*;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendAudio;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardHide;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;


public class SimpleBot extends TelegramLongPollingBot {

    boolean statusOfGame = false, riskGame = false;
    int errorCounter = 0, letterCounter = 0;
    String word = "", enteredLetters = "", wrongLetters = "";

    static ArrayList<String> wordMas = new ArrayList<String>();
    static LinkedHashMap<Integer, String> pictures = new LinkedHashMap<Integer, String>();
    static ReplyKeyboardHide keyboardHide = new ReplyKeyboardHide();
    static Connection dbConnection = null;

    public static void main(String[] args) {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        //BotDB db = new BotDB();
        try {
            keyboardHide.setHideKeyboard(true);
            wordsDict();
            picturesPath();
            if ((dbConnection == null) || (dbConnection.isClosed()))
                dbConnection = getDBConnection();
            telegramBotsApi.registerBot(new SimpleBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (SQLException r) {
            r.printStackTrace();
        }
        /*finally {
            try {
                dbConnection.close();
            } catch (SQLException ignore) {
            }
        }*/
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

    private static void wordsDict() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            File f = new File("G:\\Gallows\\words.txt");
            BufferedReader fin = new BufferedReader(new FileReader(f));
            String line;
            boolean flag = false;
            while ((line = fin.readLine()) != null)
                if ((line.length() > 3) && (line.length() < 16)) {
                    line = line.replaceAll(" ", "");
                    for (int i = 0; i < line.length(); i++)
                        for (char c = 'а'; c <= 'я'; c++)
                            if (line.charAt(i) != c)
                                flag = false;
                            else
                                flag = true;
                    if (flag = true) wordMas.add(line);
                }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void picturesPath() {
        String path;
        for (int i = 7; i > 0; ) {
            for (int j = 0; j < 7; j++) {
                path = String.format("G:\\Gallows\\%d.jpg", i);
                pictures.put(j, path);
                i--;
            }
        }
    }

    public String getBotUsername() {
        return "JARVIS";
    }

    public String getBotToken() {
        return "192433777:AAGiEH5-ylz8C8FuIQtJxgQvqDTjCezPRx0";
    }

    private void addNewUser_DB(Message message, String helpText, String commandsText) {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Long userChatId = message.getChatId();
        String selectTableSQL = "SELECT num_games FROM \"Statistics\" WHERE chat_id = " + userChatId + ";";
        ReplyKeyboardMarkup r = alphabetKeyboard();
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
            } else {
                getCurGameInfo_DB(message.getChatId());
                if (statusOfGame != false) {
                    gameState(message, "\uD83D\uDCA1 Вы еще на ходитесь в игре. Вся история и статистика игр сохранена.");
                }
                else
                sendMsgWithBtn(message, r, "\uD83D\uDCA1 Вы уже вели переписку с Hangman Bot. Вся история и статистика игр сохранена.");
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

    private void addWord_DB(Long userChatId) {
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

    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        /*sendMessage.setReplayToMessageId(message.getMessageId());*/ // отправка ответа с исх. сообщением
        sendMessage.setText(text);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsgWithBtn(Message message, ReplyKeyboard kboardB, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);
        sendMessage.setReplayMarkup(kboardB);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendImage(Message message, String passToFile) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(message.getChatId().toString());
        sendPhoto.setNewPhoto(passToFile, ".jpg");
        ReplyKeyboardHide no = new ReplyKeyboardHide();
        no.setHideKeyboard(true);
        sendPhoto.setReplayMarkup(no);
        try {
            sendPhoto(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendAudioMsg(Message message, String passToFile, String audioName) {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setChatId(message.getChatId().toString());
        sendAudio.setNewAudio(passToFile, audioName);
        try {
            sendAudio(sendAudio);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private KeyboardRow makeAlphabetRow(String line) {
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < line.length(); i++) {
            KeyboardButton kb = new KeyboardButton(Character.toString(line.charAt(i)));
            row.add(kb);
        }
        return row;
    }

    private String alphabetTest(String line) {
        String entyet = enteredLetters + wrongLetters, alphabetLine = "";
        for (int i = 0; i < line.length(); i++)
            if (entyet.indexOf(line.charAt(i)) < 0)
                alphabetLine += line.charAt(i);
        return alphabetLine;
    }

    private KeyboardRow makeSeveralButtonRow(ArrayList<String> textOfButtons) {
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < textOfButtons.size(); i++) {
            KeyboardButton btn = new KeyboardButton(textOfButtons.get(i));
            row.add(btn);
        }
        return row;
    }

    private KeyboardRow makeOneButtonRow(String text) {
        KeyboardButton btn = new KeyboardButton();
        btn.setText(text);
        KeyboardRow row = new KeyboardRow();
        row.add(btn);
        return row;
    }

    private InlineKeyboardMarkup inlineKbrd(String text, String url) {
        InlineKeyboardButton inlBttn = new InlineKeyboardButton();
        inlBttn.setText(text);
        inlBttn.setUrl(url);
        List<List<InlineKeyboardButton>> keybrdList = new ArrayList<List<InlineKeyboardButton>>();
        List<InlineKeyboardButton> btns = new ArrayList<InlineKeyboardButton>();
        btns.add(inlBttn);
        keybrdList.add(btns);
        InlineKeyboardMarkup inlKbrd = new InlineKeyboardMarkup();
        inlKbrd.setKeyboard(keybrdList);
        return inlKbrd;
    }

    private ReplyKeyboardMarkup alphabetKeyboard() {
        ArrayList<String> listOfButtons = new ArrayList<String>();
        String alphabet01 = "ЙЦУКЕНГШЩЗХ", alphabet1 = alphabetTest(alphabet01),
                alphabet02 = "ФЫВАПРОЛДЖЭ", alphabet2 = alphabetTest(alphabet02),
                alphabet03 = "ЯЧСМИТЬЪБЮ", alphabet3 = alphabetTest(alphabet03),
                riskButton = "⚡Risk!", stopGameButton = "STOP❌Game",
                commandsButton = "\u2753Help & Commands";
        listOfButtons.add(riskButton);
        listOfButtons.add(stopGameButton);
        KeyboardRow firstRow = makeAlphabetRow(alphabet1), secondRow = makeAlphabetRow(alphabet2), thirdRow = makeAlphabetRow(alphabet3),
                fourthRow = makeSeveralButtonRow(listOfButtons), fifthRow = makeOneButtonRow(commandsButton);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);
        keyboard.add(fifthRow);
        ReplyKeyboardMarkup replyKbrd = new ReplyKeyboardMarkup();
        replyKbrd.setKeyboard(keyboard);
        return replyKbrd;
    }

    private ReplyKeyboardMarkup riskButtonsMenu() {
        ArrayList<String> listOfButtons = new ArrayList<String>();
        String cancel = "\u21A9 Back", stopGame = "STOP❌Game";
        listOfButtons.add(cancel);
        listOfButtons.add(stopGame);
        KeyboardRow firstRow = makeSeveralButtonRow(listOfButtons);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        keyboard.add(firstRow);
        ReplyKeyboardMarkup replyKbrd = new ReplyKeyboardMarkup();
        replyKbrd.setKeyboard(keyboard);
        return replyKbrd;
    }

    private ReplyKeyboardMarkup mainButtonsMenu() {
        ArrayList<String> listOfButtons = new ArrayList<String>();
        String help = "❓Help & Commands", rules = "\uD83D\uDCCB Rules", start = "\uD83C\uDFAE New Game", results = "\uD83D\uDCCA Rank", about = "\uD83D\uDCD6 About";
        listOfButtons.add(help);
        listOfButtons.add(rules);
        KeyboardRow firstRow = makeOneButtonRow(start), secondRow = makeSeveralButtonRow(listOfButtons),
                thirdRow = makeOneButtonRow(results), fourthRow = makeOneButtonRow(about);
        List<KeyboardRow> keyboard = new ArrayList<KeyboardRow>();
        keyboard.add(firstRow);
        keyboard.add(secondRow);
        keyboard.add(thirdRow);
        keyboard.add(fourthRow);
        ReplyKeyboardMarkup replyKbrd = new ReplyKeyboardMarkup();
        replyKbrd.setKeyboard(keyboard);
        return replyKbrd;
    }

    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        updateMsgCmnds(message);
    }

    public void updateMsgCmnds(Message message) {
        String helpText, commandsText, rulesText, Command = "";
        List<String> commandsList = Arrays.asList("/start", "/help", "/rules", "/startgame", "/stopgame", "/results", "/risk");
        helpText = "Привет, меня зовут Hangman Bot.\nЯ сыграю с тобой в игру \"Виселица\".\n" +
                "Рискни здоровьем \uD83D\uDE28. Шутка. \uD83D\uDE02 Ха-ха-ха.\n";
        commandsText = "Комманды для взаимодействия с ботом. Все команды отправляются отдельным сообщением.\n" +
                "\u2754  /help - справка, команды для бота.\n" +
                "\uD83D\uDCCB  /rules - правила игры.\n" +
                "\uD83C\uDFAE  /startgame - начать игру.\n" +
                "\u26A1  /risk - ввести слово полностью.\n" +
                "\uD83D\uDD34  /stopgame - остановить игру, т.е. \uD83D\uDC00 сдаться.\n" +
                "\uD83D\uDCCA  /results - общий счет игры (\uD83C\uDFC6 победы и поражения\uD83D\uDCA9).\n" +
                "\uD83D\uDCD6  /about - о игре.";
        rulesText = "ПРАВИЛА игры :\nЯ (JARVIS \uD83D\uDE1C) загадываю слово, которое является существительным ед. числа(мн., если ед. нету). " +
                "Вы пытаетесь отгадать слово каждый раз вписывая предполагаемую букву. " +
                "Если буква присутствует в слове, то она вставляется в тех местах, где присутствует. " +
                "Если такой буквы нету, то количество ваших \"Жизней\"(❤) становится меньше на одну еденицу. " +
                "Если количество \"Жизней\"(❤) исчерпано, то вы проиграли. " +
                "Количество \"Жизней\"(❤) равно 7⃣. \u26A0Принудительная остановка игры означает автоматический проигрыш.⚠\n";

        if (message != null && message.hasText()) {

            if ((message.getText().charAt(0) == '/') && (commandsList.indexOf(message.getText()) < 0)) {
                int j = 0, k = 0;
                for (int i = 0; i < commandsList.size(); i++)
                    if (commandsList.get(i).indexOf(message.getText().substring(2)) > -1) {
                        j++;
                        k = i;
                    }
                if (j == 1) Command = commandsList.get(k);
            } else Command = message.getText();

            switch (Command) {
                case "/start":
                    addNewUser_DB(message, helpText, commandsText);
                    break;
                case "❓Help & Commands":
                case "/help":
                    sendMsg(message, commandsText);
                    break;
                case "\uD83D\uDCCB Rules":
                case "/rules":
                    sendMsg(message, rulesText);
                    break;
                case "\uD83C\uDFAE New Game":
                case "/startgame":
                    getStatusOfGame_DB(message.getChatId());
                    if (statusOfGame != true) {
                        statusOfGame = true;
                        firstStep(message);
                    } else
                        sendMsg(message, "Вы уже находитесь в игре‼");
                    break;
                case "STOP❌Game":
                case "/stopgame":
                    getStatusOfGame_DB(message.getChatId());
                    if (statusOfGame != false) {
                        statusOfGame = false;
                        gameOver(message);
                    } else
                        sendMsg(message, "Вы пока не находитесь в игре‼");
                    break;
                case "⚡Risk!":
                case "/risk":
                    getCurGameInfo_DB(message.getChatId());
                    if (statusOfGame != false) {
                        riskGame = true;
                        updateCurGameInfo_DB(message.getChatId());
                        ReplyKeyboardMarkup riskBtnMenu = riskButtonsMenu();
                        sendMsgWithBtn(message, riskBtnMenu, "Введите слово полностью и без ошибок. Иначе - проигрышь‼");
                    } else sendMsg(message, "Вы не находитесь в игре‼");
                    break;
                case "\u21A9 Back":
                    getCurGameInfo_DB(message.getChatId());
                    if ((statusOfGame != false) && (riskGame != false)) {
                            riskGame = false;
                            updateCurGameInfo_DB(message.getChatId());
                            gameState(message, "\uD83D\uDCA1 Вы вернулись в игру.");
                        }
                        else sendMsg(message, "В данный момент эта команда не активна.");;
                    break;
                case "\uD83D\uDCCA Rank":
                case "/results":
                    getStatusOfGame_DB(message.getChatId());
                    if (statusOfGame != true)
                        getResults(message);
                    else
                        sendMsg(message, "Закончите \uD83C\uDFC1 текущую игру, что бы узнать общий счет.");
                    break;
                case "\uD83D\uDCD6 About":
                case "/about":
                    sendMsg(message, "Привет! Меня зовут Hangman Bot.\nЯ сыграю с тобой в игру \"Виселица\".\nМой отец https://telegram.me/AndrewAbramchuk.");
                    break;
                default:
                    getCurGameInfo_DB(message.getChatId());
                    if (statusOfGame != false) {
                        if (riskGame != false) {
                            if (message.getText().length() != word.length())
                                sendMsg(message, "\uD83D\uDE27 Неверное количество букв. Введите слово заново. И внимательнее!\uD83D\uDC40");
                            else wholeWord(message);
                        } else if (message.getText().length() < 2) {
                            char c = message.getText().toUpperCase().charAt(0);
                            if (c == 'Ё') c = 'Е';
                            if ((c >= 'А') && (c <= 'Я')) {
                                if ((enteredLetters.indexOf(c) < 0) && (wrongLetters.indexOf(c) < 0))
                                    secondStep(message, String.valueOf(c));
                                else if (wrongLetters.indexOf(c) < 0)
                                    gameState(message, "\uD83D\uDE45 Введенная буква уже присутствует.");
                                else
                                    gameState(message, "\uD83D\uDE45 Введенная буква уже вводилась и не входит в слово.");
                            } else
                                gameState(message, "Введенный символ не является буквой. \uD83D\uDD20\uD83D\uDD21");
                        } else sendMsg(message, "Введите только \u0031\u20E3 букву.");
                    } else
                        sendMsg(message, "\uD83D\uDCA1 Для взаимодействия с ботом воспользуйтесь его командами. (❓Help & Commands)");
                    break;
            }
        }
    }

    private void firstStep(Message message) {
        word = wordChoice(message).toUpperCase();
        errorCounter = 7;
        if (word.length() > 9)
            letterClue(word);
        letterCounter = word.length();
        initNewGame_DB(message.getChatId());
        gameState(message, String.format("Слово из " + Integer.toString(word.length()) + " букв."));
    }

    private void secondStep(Message message, String letter) {
        String finalText = "";
        boolean errorFlag = true;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == letter.charAt(0)) {
                enteredLetters += word.charAt(i);
                letterCounter -= 1;
                errorFlag = false;
            }
        }
        if (errorFlag != false) {
            errorCounter -= 1;
            if (errorCounter < 1)
                gameOver(message);
            else {
                if (wrongLetters.indexOf(letter.charAt(0)) < 0)
                    wrongLetters += letter.charAt(0);
                updateCurGameInfo_DB(message.getChatId());
                sendImage(message, pictures.get(errorCounter));
                gameState(message, "\u274C Вы ошиблись. Нет такой буквы.");
            }
        } else if (finalText.equals(word))
            win(message);
        else {
            updateCurGameInfo_DB(message.getChatId());
            gameState(message, "\u2705 Есть такая буква.");
        }
    }

    private void gameState(Message message, String comment) {
        String cellularWord = "", live = "";
        for (int i = 0; i < word.length(); i++)
            if (enteredLetters.indexOf(word.charAt(i)) < 0)
                cellularWord += "\u25FD";
            else cellularWord += word.charAt(i);
        for (int i = 0; i < errorCounter; i++)
            live += "\u2764";
        String textMsg = comment + "\n" + "Слово: " + cellularWord + "\n" + "Жизнь: " + live;
        ReplyKeyboardMarkup r = alphabetKeyboard();
        sendMsgWithBtn(message, r, textMsg);
    }

    private void wholeWord(Message message) {
        if (!word.equals(message.getText().toUpperCase())) {
            sendMsg(message, "\uD83D\uDE22 Увы, не верно.");
            sendAudioMsg(message, "G:\\Gallows\\loser.mp3", "Words of cheer");
            gameOver(message);
        } else {
            sendMsg(message, "\uD83D\uDE00 " + message.getFrom().getFirstName() + ", да вы Гений!! ");
            sendAudioMsg(message, "G:\\Gallows\\winner.mp3", "Hallelujah!");
            win(message);
        }
    }

    private void gameOver(Message message) {
        String textOfButton = "\uD83D\uDCD1 Узнать значение слова",
                urlOfButton = "http://sanstv.ru/dictionary/word-" + word;
        InlineKeyboardMarkup inlKbrd = inlineKbrd(textOfButton, urlOfButton);
        if ((errorCounter != 0) && (riskGame != true)) {
            sendImage(message, "G:\\Gallows\\coward.jpg");
        } else {
            sendImage(message, pictures.get(0));
            sendImage(message, "G:\\Gallows\\gameover.jpg");
        }
        ReplyKeyboardMarkup r = mainButtonsMenu();
        sendMsgWithBtn(message, r, "\uD83D\uDCA9 GAME OVER \uD83D\uDCA9" + "\n");
        sendMsgWithBtn(message, inlKbrd, "\nЗагаданное слово: " + word);
        exit(message, false);
    }

    private void win(Message message) {
        String textOfButton = "\uD83D\uDCD1 Узнать значение слова",
                urlOfButton = "http://sanstv.ru/dictionary/word-" + word;
        InlineKeyboardMarkup inlKbrd = inlineKbrd(textOfButton, urlOfButton);
        ReplyKeyboardMarkup r = mainButtonsMenu();
        sendImage(message, "G:\\Gallows\\win.jpg");
        sendMsgWithBtn(message, r, "\n \uD83D\uDC51 You Win. LIVE ON! \uD83D\uDC51");
        sendMsgWithBtn(message, inlKbrd, "\nЗагаданное слово: " + word);
        exit(message, true);
    }

    private void exit(Message message, boolean outcome) {
        deleteCurrentGame_DB(message.getChatId());
        addWord_DB(message.getChatId());
        updateStatistics_DB(message.getChatId(), outcome);
        statusOfGame = false;
        errorCounter = 0;
        letterCounter = 0;
        word = "";
        enteredLetters = "";
        wrongLetters = "";
        riskGame = false;
    }

    private void getResults(Message message) {
        int[] statMas = getStatistics_DB(message.getChatId());
        String text = " ВАШ РЕЗУЛЬТАТ: \uD83D\uDCC8\uD83D\uDCC9\uD83D\uDCC8\nИгр проведено - " + statMas[0] +
                "\n\uD83D\uDC4D  Выигранных" + " - " + statMas[1] +
                "\nПроигранных " + "\uD83D\uDC4E - " + statMas[2];
        sendMsg(message, text);
    }

    private String wordChoice(Message message) {
        String myWord = "";
        while (myWord.length() < 1) {
            String w = randWord();
            int temp = getWordCheck_DB(message.getChatId(), w);
            if (temp < 1) myWord = w;
        }
        return myWord;
    }

    private String randWord() {
        Random rand = new Random();
        int number = rand.nextInt(wordMas.size());
        String myWord = wordMas.get(number);
        return myWord;
    }

    private void letterClue(String myWord) {
        int error = 0;
        String str1 = "";
        // Подсказки будут, только если кол-во неповторяющихся букв будет больше 2/3 слова. И строго на лишнее кол-во.
        for (int i = 0; i < myWord.length(); i++)
            if (str1.indexOf(myWord.charAt(i)) < 0) str1 += myWord.charAt(i);
        Double exp1 = (double) str1.length() / myWord.length(),
                exp2 = 1 / (double) myWord.length(),
                exp3 = exp1 - 0.67;
        if (exp3 > 0) error = (int) Math.ceil(exp3 / exp2);
        Random rand = new Random();
        int number = 0;
        for (int i = 0; i < error; i++) {
            number = rand.nextInt(str1.length());
            if (myWord.indexOf(str1.charAt(number)) == myWord.lastIndexOf(str1.charAt(number)))
                enteredLetters += str1.charAt(number);
            else i--;
        }
        enteredLetters = enteredLetters.toUpperCase();
    }
}