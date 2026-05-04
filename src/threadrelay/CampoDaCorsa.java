package threadrelay;

import java.awt.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import javax.swing.*;

public class CampoDaCorsa extends JFrame implements Observer {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CampoDaCorsa.class.getName());
    private static final int RUNNER_COUNT = 4;
    private static final int MAX_PROGRESS = 99;

    private final int[] progress = new int[RUNNER_COUNT];
    private final AtomicIntegerArray progressModel = new AtomicIntegerArray(RUNNER_COUNT);
    private final JLabel[] valueLabels = new JLabel[RUNNER_COUNT];
    private final Thread[] runnerThreads = new Thread[RUNNER_COUNT];
    final Object pauseLock = new Object();

    private volatile boolean running;
    volatile boolean paused;
    volatile boolean stopRequested;

    private RacePanel racePanel;
    private JComboBox<String> velocitaDropdown;
    private JButton avviaButton;
    private JButton sospendeButton;
    private JButton riprendeButton;
    private JButton fermaButton;

    public CampoDaCorsa() {
        initComponents();
    }

    private void initComponents() {
        setTitle("ThreadRelay");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setBackground(new Color(28, 32, 36));
        setContentPane(content);

        JPanel centerWrapper = new JPanel(new BorderLayout(10, 0));
        centerWrapper.setOpaque(false);

        racePanel = new RacePanel();
        centerWrapper.add(racePanel, BorderLayout.CENTER);
        centerWrapper.add(createInfoPanel(), BorderLayout.EAST);
        content.add(centerWrapper, BorderLayout.CENTER);
        content.add(createControlsPanel(), BorderLayout.SOUTH);
        bindActions();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(185, 312));
        infoPanel.setBackground(new Color(40, 44, 52));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < RUNNER_COUNT; i++) {
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

            JLabel runnerName = new JLabel("Runner " + (i + 1));
            runnerName.setForeground(Color.WHITE);
            runnerName.setFont(new Font("Segoe UI", Font.BOLD, 15));

            JLabel value = new JLabel("0");
            value.setHorizontalAlignment(SwingConstants.RIGHT);
            value.setForeground(new Color(90, 220, 255));
            value.setFont(new Font("Consolas", Font.BOLD, 20));

            row.add(runnerName, BorderLayout.NORTH);
            row.add(value, BorderLayout.SOUTH);
            infoPanel.add(row);

            valueLabels[i] = value;
        }

        return infoPanel;
    }

    private JPanel createControlsPanel() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.setBackground(new Color(48, 53, 60));
        controls.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        velocitaDropdown = new JComboBox<>(new String[]{"Slow", "Regular", "Fast"});
        velocitaDropdown.setPreferredSize(new Dimension(110, 28));

        avviaButton = new JButton("Avvia");
        sospendeButton = new JButton("Sospende");
        riprendeButton = new JButton("Riprende");
        fermaButton = new JButton("Ferma");

        sospendeButton.setEnabled(false);
        riprendeButton.setEnabled(false);
        fermaButton.setEnabled(false);

        controls.add(velocitaDropdown);
        controls.add(avviaButton);
        controls.add(sospendeButton);
        controls.add(riprendeButton);
        controls.add(fermaButton);

        return controls;
    }

    private void bindActions() {
        avviaButton.addActionListener(e -> avviaSimulazione());
        sospendeButton.addActionListener(e -> sospendiSimulazione());
        riprendeButton.addActionListener(e -> riprendiSimulazione());
        fermaButton.addActionListener(e -> fermaSimulazione());
    }

    private void avviaSimulazione() {
        if (running) {
            return;         // ← evita doppio avvio
        }
        running = true;              // ← segna che la gara è in corso
        paused = false;
        stopRequested = false;
        resetRace();                 // ← resetta la grafica
        aggiornaControlliInCorsa();  // ← abilita Sospendi e Ferma

        Thread coordinatore = new Thread(() -> {
            Gestore gestore = new Gestore(this);
            gestore.avviaGara();
            running = false;
            paused = false;
            stopRequested = false;
            aggiornaControlliFineGara();
        }, "relay-coordinator");
        coordinatore.setDaemon(true);
        coordinatore.start();
    }

    private void sospendiSimulazione() {
        if (!running || paused) {
            return;
        }
        paused = true;
        runOnEdt(() -> {
            sospendeButton.setEnabled(false);
            riprendeButton.setEnabled(true);
        });
    }

    private void riprendiSimulazione() {
        if (!running || !paused) {
            return;
        }
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        runOnEdt(() -> {
            sospendeButton.setEnabled(true);
            riprendeButton.setEnabled(false);
        });
    }

    private void fermaSimulazione() {
        if (!running) {
            return;
        }
        stopRequested = true;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        runOnEdt(() -> {
            sospendeButton.setEnabled(false);
            riprendeButton.setEnabled(false);
            fermaButton.setEnabled(false);
        });
    }

    public int getDelayBySelection() {
        int index = velocitaDropdown.getSelectedIndex();
        if (index == 0) {
            return 120;
        }
        if (index == 2) {
            return 35;
        }
        return 70;
    }

    private void runRace(int delayMillis) {
        for (int runner = 0; runner < RUNNER_COUNT && !stopRequested; runner++) {
            if (runner > 0) {
                waitForThreshold(runner - 1, 90);
            }
            if (!stopRequested) {
                startRunner(runner, delayMillis);
            }
        }

        for (int i = 0; i < RUNNER_COUNT; i++) {
            Thread t = runnerThreads[i];
            if (t != null) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        running = false;
        paused = false;
        stopRequested = false;
        aggiornaControlliFineGara();
    }

    private void waitForThreshold(int runnerIndex, int threshold) {
        while (!stopRequested && progressModel.get(runnerIndex) < threshold) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void startRunner(int runnerIndex, int delayMillis) {
        Thread t = new Thread(() -> runSingleRunner(runnerIndex, delayMillis), "runner-" + (runnerIndex + 1));
        runnerThreads[runnerIndex] = t;
        t.start();
    }

    private void runSingleRunner(int runnerIndex, int delayMillis) {
        for (int value = 1; value <= MAX_PROGRESS; value++) {
            if (!waitWhilePaused()) {
                return;
            }
            if (stopRequested) {
                return;
            }
            setRunnerProgress(runnerIndex, value);
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!stopRequested) {
            setRunnerFinished(runnerIndex);
        }
    }

    private boolean waitWhilePaused() {
        synchronized (pauseLock) {
            while (paused && !stopRequested) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return !stopRequested;
    }

    private void aggiornaControlliInCorsa() {
        runOnEdt(() -> {
            avviaButton.setEnabled(false);
            velocitaDropdown.setEnabled(false);
            sospendeButton.setEnabled(true);
            riprendeButton.setEnabled(false);
            fermaButton.setEnabled(true);
        });
    }

    private void aggiornaControlliFineGara() {
        runOnEdt(() -> {
            avviaButton.setEnabled(true);
            velocitaDropdown.setEnabled(true);
            sospendeButton.setEnabled(false);
            riprendeButton.setEnabled(false);
            fermaButton.setEnabled(false);
        });
    }

    public void setRunnerProgress(int runnerIndex, int value) {
        if (runnerIndex < 0 || runnerIndex >= RUNNER_COUNT) {
            throw new IllegalArgumentException("Indice runner non valido: " + runnerIndex);
        }

        int normalized = Math.max(0, Math.min(MAX_PROGRESS, value));
        progressModel.set(runnerIndex, normalized);
        runOnEdt(() -> {
            progress[runnerIndex] = normalized;
            valueLabels[runnerIndex].setText(String.valueOf(normalized));
            racePanel.repaint();
        });
    }

    public void setRunnerFinished(int runnerIndex) {
        if (runnerIndex < 0 || runnerIndex >= RUNNER_COUNT) {
            throw new IllegalArgumentException("Indice runner non valido: " + runnerIndex);
        }

        progressModel.set(runnerIndex, MAX_PROGRESS);
        runOnEdt(() -> {
            progress[runnerIndex] = MAX_PROGRESS;
            valueLabels[runnerIndex].setText("Fine");
            racePanel.repaint();
        });
    }

    public void resetRace() {
        runOnEdt(() -> {
            for (int i = 0; i < RUNNER_COUNT; i++) {
                progress[i] = 0;
                progressModel.set(i, 0);
                runnerThreads[i] = null;
                valueLabels[i].setText("0");
            }
            racePanel.repaint();
        });
    }

    public JComboBox<String> getVelocitaDropdown() {
        return velocitaDropdown;
    }

    public JButton getAvviaButton() {
        return avviaButton;
    }

    public JButton getSospendeButton() {
        return sospendeButton;
    }

    public JButton getRiprendeButton() {
        return riprendeButton;
    }

    public JButton getFermaButton() {
        return fermaButton;
    }

    private void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void main(String args[]) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new CampoDaCorsa().setVisible(true));
    }

    @Override
    public void update(int runnerIndex, int valore) {
        System.out.println("UPDATE ricevuto: runner=" + runnerIndex + " valore=" + valore);
        if (valore >= 100) {
            setRunnerFinished(runnerIndex);
        } else {
            setRunnerProgress(runnerIndex, valore);
        }
    }

    private final class RacePanel extends JPanel {

        private static final int TRACK_HEIGHT = 70;
        private static final int RUNNER_SIZE = 24;
        private final Color[] laneColors = {
            new Color(249, 117, 117),
            new Color(113, 201, 255),
            new Color(255, 204, 102),
            new Color(151, 242, 174)
        };

        RacePanel() {
            setPreferredSize(new Dimension(760, 312));
            setBackground(new Color(62, 66, 76));
            setBorder(BorderFactory.createLineBorder(new Color(95, 104, 116), 1, true));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Insets insets = getInsets();
            int left = insets.left + 14;
            int top = insets.top + 12;
            int width = getWidth() - insets.left - insets.right - 28;

            for (int i = 0; i < RUNNER_COUNT; i++) {
                int y = top + (i * TRACK_HEIGHT);
                drawLane(g2, i, left, y, width);
            }

            g2.dispose();
        }

        private void drawLane(Graphics2D g2, int index, int x, int y, int width) {
            g2.setColor(new Color(85, 91, 103));
            g2.fillRoundRect(x, y, width, 52, 14, 14);

            g2.setColor(new Color(109, 117, 131));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(x, y, width, 52, 14, 14);

            g2.setColor(new Color(180, 187, 199));
            g2.drawLine(x + 8, y + 26, x + width - 8, y + 26);

            int travelWidth = width - RUNNER_SIZE - 16;
            int runnerX = x + 8 + (progress[index] * travelWidth / MAX_PROGRESS);
            int runnerY = y + 14;

            g2.setColor(laneColors[index]);
            g2.fillOval(runnerX, runnerY, RUNNER_SIZE, RUNNER_SIZE);

            g2.setColor(new Color(24, 27, 31));
            g2.drawOval(runnerX, runnerY, RUNNER_SIZE, RUNNER_SIZE);

            g2.setColor(new Color(220, 226, 235));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.drawString("R" + (index + 1), x + 12, y + 18);
        }
    }
}
