import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.io.*;
import java.util.Scanner;
import java.util.UUID;
import com.google.gson.*;

public class oreminer extends JFrame {
    private static final int SIZE = 10;
    private static final String HIGHSCORE_FILE = "highscore.json";
    private static final String SEED_FILE = "seeds.json";
    private static final String SETTINGS_FILE = "settings.json";
    
    private JButton[][] gridButtons;
    private int[][] oreGrid;
    private int totalScore = 0;
    private int highScore = 0;
    private JLabel scoreLabel;
    private JLabel highScoreLabel;
    private JPanel gamePanel;
    private GameSettings settings;
    private Gson gson;

    private enum Difficulty {
        EASY(60, 80, 90, 96, 99),
        NORMAL(50, 70, 85, 93, 98),
        HARD(40, 60, 75, 85, 95);

        final int[] thresholds;

        Difficulty(int... thresholds) {
            this.thresholds = thresholds;
        }
    }

    private static class GameSettings {
        private Difficulty difficulty = Difficulty.NORMAL;
        private boolean soundEnabled = true;
        private boolean animationsEnabled = true;
        private String theme = "Default";

        public GameSettings() {}
    }

    private static class SeedData {
        private String seedId;
        private int[][] oreData;
        private long timestamp;

        public SeedData(String seedId, int[][] oreData) {
            this.seedId = seedId;
            this.oreData = oreData;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public oreminer() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        loadSettings();
        loadHighScore();
        
        setTitle("Ore Miner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveHighScore();
                saveSettings();
                System.exit(0);
            }
        });
        setLayout(new BorderLayout());

        createMenuBar();
        createScorePanel();
        createGamePanel();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadSettings() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                settings = gson.fromJson(reader, GameSettings.class);
                reader.close();
                if (settings == null) {
                    settings = new GameSettings();
                }
            } else {
                settings = new GameSettings();
            }
        } catch (Exception e) {
            settings = new GameSettings();
        }
    }

    private void saveSettings() {
        try {
            FileWriter writer = new FileWriter(SETTINGS_FILE);
            gson.toJson(settings, writer);
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Error saving settings: " + e.getMessage(), 
                "Save Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHighScore() {
        try {
            File file = new File(HIGHSCORE_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                reader.close();
                if (json != null && json.has("highScore")) {
                    highScore = json.get("highScore").getAsInt();
                }
            }
        } catch (Exception e) {
            highScore = 0;
        }
    }

    private void saveHighScore() {
        if (totalScore > highScore) {
            highScore = totalScore;
        }
        try {
            FileWriter writer = new FileWriter(HIGHSCORE_FILE);
            JsonObject json = new JsonObject();
            json.addProperty("highScore", highScore);
            gson.toJson(json, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(e -> resetGame(false));

        JMenuItem importSeedItem = new JMenuItem("Import Seed");
        importSeedItem.addActionListener(e -> showSeedImportDialog());

        JMenuItem viewSeedsItem = new JMenuItem("View All Seeds");
        viewSeedsItem.addActionListener(e -> showAllSeeds());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            saveHighScore();
            saveSettings();
            System.exit(0);
        });

        gameMenu.add(newGameItem);
        gameMenu.add(importSeedItem);
        gameMenu.add(viewSeedsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu difficultyMenu = new JMenu("Difficulty");
        ButtonGroup difficultyGroup = new ButtonGroup();

        for (Difficulty diff : Difficulty.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(diff.name());
            item.setSelected(settings.difficulty == diff);
            item.addActionListener(e -> {
                settings.difficulty = diff;
                saveSettings();
                JOptionPane.showMessageDialog(this, 
                    "Difficulty set to " + diff.name() + "\nStart a new game for this to take effect.", 
                    "Difficulty Changed", 
                    JOptionPane.INFORMATION_MESSAGE);
            });
            difficultyGroup.add(item);
            difficultyMenu.add(item);
        }

        JMenu settingsMenu = new JMenu("Settings");
        
        JCheckBoxMenuItem soundItem = new JCheckBoxMenuItem("Sound Effects", settings.soundEnabled);
        soundItem.addActionListener(e -> {
            settings.soundEnabled = soundItem.isSelected();
            saveSettings();
        });

        JCheckBoxMenuItem animationsItem = new JCheckBoxMenuItem("Animations", settings.animationsEnabled);
        animationsItem.addActionListener(e -> {
            settings.animationsEnabled = animationsItem.isSelected();
            saveSettings();
        });

        JMenuItem resetHighScoreItem = new JMenuItem("Reset High Score");
        resetHighScoreItem.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset the high score?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                highScore = 0;
                highScoreLabel.setText("High Score: 0");
                saveHighScore();
                JOptionPane.showMessageDialog(this, 
                    "High score has been reset!", 
                    "Reset Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JMenuItem clearSeedsItem = new JMenuItem("Clear All Seeds");
        clearSeedsItem.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete all saved seeds?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.YES_OPTION) {
                try {
                    FileWriter writer = new FileWriter(SEED_FILE);
                    gson.toJson(new JsonArray(), writer);
                    writer.close();
                    JOptionPane.showMessageDialog(this, 
                        "All seeds have been cleared!", 
                        "Clear Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error clearing seeds: " + ex.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        settingsMenu.add(soundItem);
        settingsMenu.add(animationsItem);
        settingsMenu.addSeparator();
        settingsMenu.add(resetHighScoreItem);
        settingsMenu.add(clearSeedsItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());

        JMenuItem instructionsItem = new JMenuItem("Instructions");
        instructionsItem.addActionListener(e -> showInstructions());

        helpMenu.add(instructionsItem);
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(difficultyMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void createScorePanel() {
        JPanel scorePanel = new JPanel();
        scorePanel.setLayout(new GridLayout(2, 1));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        scoreLabel.setForeground(Color.BLACK);

        highScoreLabel = new JLabel("High Score: " + highScore, SwingConstants.CENTER);
        highScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        highScoreLabel.setForeground(Color.BLACK);

        scorePanel.add(scoreLabel);
        scorePanel.add(highScoreLabel);

        add(scorePanel, BorderLayout.SOUTH);
    }

    private void createGamePanel() {
        oreGrid = new int[SIZE][SIZE];
        gridButtons = new JButton[SIZE][SIZE];

        initializeOreGrid();
        generateSeed();

        gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(SIZE, SIZE));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                JButton button = new JButton("Mine");
                button.setPreferredSize(new Dimension(65, 65));
                button.setBackground(Color.LIGHT_GRAY);
                button.setForeground(Color.BLACK);
                button.setFont(new Font("SansSerif", Font.BOLD, 11));
                button.addActionListener(new OreButtonListener(row, col));
                gridButtons[row][col] = button;
                gamePanel.add(button);
            }
        }

        add(gamePanel, BorderLayout.CENTER);
    }

    private void initializeOreGrid() {
        Random random = new Random();
        int[] thresholds = settings.difficulty.thresholds;
        
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int rand = random.nextInt(100);
                if (rand < thresholds[0]) {
                    oreGrid[row][col] = 0;
                } else if (rand < thresholds[1]) {
                    oreGrid[row][col] = 1;
                } else if (rand < thresholds[2]) {
                    oreGrid[row][col] = 2;
                } else if (rand < thresholds[3]) {
                    oreGrid[row][col] = 3;
                } else if (rand < thresholds[4]) {
                    oreGrid[row][col] = 4;
                } else {
                    oreGrid[row][col] = 5;
                }
            }
        }
    }

    private void generateSeed() {
        String seedId = UUID.randomUUID().toString();
        SeedData seed = new SeedData(seedId, oreGrid);

        JsonArray seedList = loadSeedList();
        JsonElement seedElement = gson.toJsonTree(seed);
        seedList.add(seedElement);

        try {
            FileWriter writer = new FileWriter(SEED_FILE);
            gson.toJson(seedList, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving seed: " + e.getMessage());
        }
    }

    private JsonArray loadSeedList() {
        try {
            File file = new File(SEED_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                JsonArray array = gson.fromJson(reader, JsonArray.class);
                reader.close();
                return array != null ? array : new JsonArray();
            }
        } catch (Exception e) {
            System.err.println("Error loading seeds: " + e.getMessage());
        }
        return new JsonArray();
    }

    private void showSeedImportDialog() {
        String seedId = JOptionPane.showInputDialog(this, 
            "Enter Seed ID:", 
            "Import Seed", 
            JOptionPane.PLAIN_MESSAGE);
        if (seedId != null && !seedId.trim().isEmpty()) {
            importSeed(seedId.trim());
        }
    }

    private void showAllSeeds() {
        JsonArray seedList = loadSeedList();
        if (seedList.size() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No seeds saved yet. Play a game to generate seeds!", 
                "No Seeds", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder seedInfo = new StringBuilder("Saved Seeds:\n\n");
        for (int i = 0; i < Math.min(seedList.size(), 10); i++) {
            JsonObject seed = seedList.get(i).getAsJsonObject();
            String id = seed.get("seedId").getAsString();
            seedInfo.append((i + 1)).append(". ").append(id).append("\n");
        }
        
        if (seedList.size() > 10) {
            seedInfo.append("\n... and ").append(seedList.size() - 10).append(" more");
        }

        JOptionPane.showMessageDialog(this, seedInfo.toString(), 
            "All Seeds", JOptionPane.INFORMATION_MESSAGE);
    }

    private void importSeed(String seedId) {
        JsonArray seedList = loadSeedList();
        SeedData selectedSeed = null;

        for (JsonElement element : seedList) {
            SeedData seed = gson.fromJson(element, SeedData.class);
            if (seedId.equals(seed.seedId)) {
                selectedSeed = seed;
                break;
            }
        }

        if (selectedSeed != null) {
            if (selectedSeed.oreData.length != SIZE || selectedSeed.oreData[0].length != SIZE) {
                JOptionPane.showMessageDialog(this, 
                    "Seed data size mismatch. Expected " + SIZE + "x" + SIZE + " grid.", 
                    "Import Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            resetGame(true);
            oreGrid = selectedSeed.oreData;
            JOptionPane.showMessageDialog(this, 
                "Seed imported successfully!\nID: " + seedId, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Seed not found. Please check the ID and try again.", 
                "Import Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetGame(boolean imported) {
        if (totalScore > highScore) {
            highScore = totalScore;
            highScoreLabel.setText("High Score: " + highScore);
            saveHighScore();
        }

        totalScore = 0;
        scoreLabel.setText("Score: 0");

        remove(gamePanel);
        
        oreGrid = new int[SIZE][SIZE];
        if (!imported) {
            initializeOreGrid();
            generateSeed();
        }

        gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(SIZE, SIZE));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                JButton button = new JButton("Mine");
                button.setPreferredSize(new Dimension(65, 65));
                button.setBackground(Color.LIGHT_GRAY);
                button.setForeground(Color.BLACK);
                button.setFont(new Font("SansSerif", Font.BOLD, 11));
                button.addActionListener(new OreButtonListener(row, col));
                gridButtons[row][col] = button;
                gamePanel.add(button);
            }
        }

        add(gamePanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
            "Ore Miner v1.3\n\n" +
            "A simple mining game where you dig for valuable ores!\n\n" +
            "Created with Java",
            "About Ore Miner",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showInstructions() {
        JOptionPane.showMessageDialog(this,
            "How to Play:\n\n" +
            "1. Click 'Mine' buttons to dig for ore\n" +
            "2. Each ore type gives different points:\n" +
            "   • Stone: 0 points\n" +
            "   • Quartz: 1 point\n" +
            "   • Copper: 2 points\n" +
            "   • Amethyst: 3 points\n" +
            "   • Gold: 4 points\n" +
            "   • Diamond: 5 points\n\n" +
            "3. Try to get the highest score possible!\n\n" +
            "Features:\n" +
            "• Save and import seeds to replay boards\n" +
            "• Adjust difficulty for more or fewer valuable ores\n" +
            "• Track your high score across games",
            "Instructions",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private String getOreName(int points) {
        switch (points) {
            case 0: return "Stone";
            case 1: return "Quartz";
            case 2: return "Copper";
            case 3: return "Amethyst";
            case 4: return "Gold";
            case 5: return "Diamond";
            default: return "Unknown";
        }
    }

    private Color getOreColor(int points) {
        switch (points) {
            case 0: return new Color(100, 100, 100);
            case 1: return new Color(255, 255, 250);
            case 2: return new Color(184, 115, 51);
            case 3: return new Color(160, 100, 220);
            case 4: return new Color(255, 215, 0);
            case 5: return new Color(170, 255, 255);
            default: return Color.WHITE;
        }
    }

    private class OreButtonListener implements ActionListener {
        private final int row;
        private final int col;

        public OreButtonListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton button = gridButtons[row][col];
            if (button.isEnabled()) {
                int points = oreGrid[row][col];
                totalScore += points;
                scoreLabel.setText("Score: " + totalScore);

                String oreName = getOreName(points);
                Color oreColor = getOreColor(points);

                String labelText = "<html><center>" + oreName + "<br>+" + points + "</center></html>";

                button.setText(labelText);
                button.setBackground(oreColor);
                button.setForeground(Color.BLACK);
                button.setEnabled(false);

                if (settings.animationsEnabled) {
                    animateButton(button);
                }

                checkGameComplete();
            }
        }

        private void animateButton(JButton button) {
            Timer timer = new Timer(50, null);
            final int[] scale = {0};
            timer.addActionListener(evt -> {
                scale[0] += 10;
                if (scale[0] >= 100) {
                    timer.stop();
                } else {
                    button.repaint();
                }
            });
            timer.start();
        }
    }

    private void checkGameComplete() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (gridButtons[row][col].isEnabled()) {
                    return;
                }
            }
        }

        if (totalScore > highScore) {
            highScore = totalScore;
            highScoreLabel.setText("High Score: " + highScore);
            saveHighScore();
        }

        String message = totalScore > 0 && totalScore == highScore ? 
            "Game Complete! New High Score: " + totalScore + "\n\nPlay again?" :
            "Game Complete! Your score: " + totalScore + "\n\nPlay again?";

        int response = JOptionPane.showConfirmDialog(this,
            message,
            "Game Over",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            resetGame(false);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new oreminer());
    }
}