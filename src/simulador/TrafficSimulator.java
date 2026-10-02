package simulador;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class TrafficSimulator extends JFrame {

    public TrafficSimulator() {
        setTitle("Simulador de Tráfego 2D - Multithread");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        SimulationPanel panel = new SimulationPanel();
        add(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TrafficSimulator sim = new TrafficSimulator();
            sim.setVisible(true);
        });
    }
}

// --- ESTADOS DO SEMÁFORO ---
enum LightState { GREEN, YELLOW, RED }

class TrafficLightController implements Runnable {
    private LightState nsState = LightState.GREEN; // Norte-Sul
    private LightState ewState = LightState.RED;   // Leste-Oeste

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Norte-Sul Verde | Leste-Oeste Vermelho
                nsState = LightState.GREEN;
                ewState = LightState.RED;
                Thread.sleep(5000);

                // Norte-Sul Amarelo
                nsState = LightState.YELLOW;
                Thread.sleep(2000);

                // Norte-Sul Vermelho | Leste-Oeste Verde
                nsState = LightState.RED;
                ewState = LightState.GREEN;
                Thread.sleep(5000);

                // Leste-Oeste Amarelo
                ewState = LightState.YELLOW;
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public LightState getNsState() { return nsState; }
    public LightState getEwState() { return ewState; }
}

// --- DIREÇÃO DOS VEÍCULOS ---
enum Direction { NORTH_TO_SOUTH, SOUTH_TO_NORTH, WEST_TO_EAST, EAST_TO_WEST }

// --- CLASSE DO VEÍCULO (THREAD INDIVIDUAL) ---
class Vehicle implements Runnable {
    private float x, y;
    private final float speed;
    private final Direction direction;
    private final Color color;
    private final SimulationPanel panel;
    private boolean running = true;

    public Vehicle(float x, float y, float speed, Direction direction, Color color, SimulationPanel panel) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.direction = direction;
        this.color = color;
        this.panel = panel;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Ponto de parada antes do cruzamento
                if (shouldStopAtIntersection()) {
                    Thread.sleep(50);
                    continue;
                }

                // Semáforo para atravessar o centro do cruzamento
                boolean crossingIntersection = isAtIntersectionArea();
                if (crossingIntersection) {
                    panel.getIntersectionSemaphore().acquire();
                }

                // Movimentação do veículo
                switch (direction) {
                    case WEST_TO_EAST: x += speed; break;
                    case EAST_TO_WEST: x -= speed; break;
                    case NORTH_TO_SOUTH: y += speed; break;
                    case SOUTH_TO_NORTH: y -= speed; break;
                }

                if (crossingIntersection) {
                    panel.getIntersectionSemaphore().release();
                }

                // Remove veículo se sair da tela
                if (x < -50 || x > 850 || y < -50 || y > 850) {
                    running = false;
                    panel.removeVehicle(this);
                }

                Thread.sleep(30);
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isAtIntersectionArea() {
        return (x >= 340 && x <= 460 && y >= 340 && y <= 460);
    }

    private boolean shouldStopAtIntersection() {
        LightState ns = panel.getLightController().getNsState();
        LightState ew = panel.getLightController().getEwState();

        if (direction == Direction.WEST_TO_EAST && x >= 310 && x < 330) {
            return ew != LightState.GREEN;
        }
        if (direction == Direction.EAST_TO_WEST && x <= 490 && x > 470) {
            return ew != LightState.GREEN;
        }
        if (direction == Direction.NORTH_TO_SOUTH && y >= 310 && y < 330) {
            return ns != LightState.GREEN;
        }
        if (direction == Direction.SOUTH_TO_NORTH && y <= 490 && y > 470) {
            return ns != LightState.GREEN;
        }

        return false;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public Color getColor() { return color; }
    public Direction getDirection() { return direction; }
}

// --- PAINEL PRINCIPAL DE SIMULAÇÃO E RENDERIZAÇÃO ---
class SimulationPanel extends JPanel {
    private final List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    private final TrafficLightController lightController = new TrafficLightController();
    private final Semaphore intersectionSemaphore = new Semaphore(2, true);
    private final Random random = new Random();

    public SimulationPanel() {
        setBackground(new Color(34, 139, 34)); // Grama verde

        // Inicia Thread do Semáforo
        new Thread(lightController).start();

        // Spawner de veículos contínuo
        Thread spawnerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    spawnVehicle();
                    Thread.sleep(1200 + random.nextInt(1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        spawnerThread.start();

        // Timer de Redesenho Gráfico (60 FPS)
        Timer renderTimer = new Timer(16, e -> repaint());
        renderTimer.start();
    }

    private void spawnVehicle() {
        Direction[] dirs = Direction.values();
        Direction dir = dirs[random.nextInt(dirs.length)];
        float speed = 2.0f + random.nextFloat() * 2.5f;
        Color[] colors = {Color.RED, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.CYAN};
        Color color = colors[random.nextInt(colors.length)];

        float x = 0, y = 0;
        boolean lane2 = random.nextBoolean();

        switch (dir) {
            case WEST_TO_EAST:
                x = -30;
                y = lane2 ? 410 : 435;
                break;
            case EAST_TO_WEST:
                x = 830;
                y = lane2 ? 365 : 390;
                break;
            case NORTH_TO_SOUTH:
                x = lane2 ? 365 : 390;
                y = -30;
                break;
            case SOUTH_TO_NORTH:
                x = lane2 ? 410 : 435;
                y = 830;
                break;
        }

        Vehicle v = new Vehicle(x, y, speed, dir, color, this);
        vehicles.add(v);
        new Thread(v).start();
    }

    public void removeVehicle(Vehicle v) {
        vehicles.remove(v);
    }

    public TrafficLightController getLightController() { return lightController; }
    public Semaphore getIntersectionSemaphore() { return intersectionSemaphore; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Asfalto
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(350, 0, 100, 800);
        g2d.fillRect(0, 350, 800, 100);

        // 2. Linhas centrais
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(400, 0, 400, 350); g2d.drawLine(400, 450, 400, 800);
        g2d.drawLine(0, 400, 350, 400); g2d.drawLine(450, 400, 800, 400);

        // 3. Semáforos
        drawTrafficLights(g2d);

        // 4. Veículos
        for (Vehicle v : vehicles) {
            g2d.setColor(v.getColor());
            if (v.getDirection() == Direction.WEST_TO_EAST || v.getDirection() == Direction.EAST_TO_WEST) {
                g2d.fillRect((int) v.getX(), (int) v.getY(), 24, 12);
                g2d.setColor(Color.BLACK);
                g2d.drawRect((int) v.getX(), (int) v.getY(), 24, 12);
            } else {
                g2d.fillRect((int) v.getX(), (int) v.getY(), 12, 24);
                g2d.setColor(Color.BLACK);
                g2d.drawRect((int) v.getX(), (int) v.getY(), 12, 24);
            }
        }
    }

    private void drawTrafficLights(Graphics2D g2d) {
        LightState ns = lightController.getNsState();
        LightState ew = lightController.getEwState();

        drawLightBox(g2d, 315, 300, ns);
        drawLightBox(g2d, 465, 460, ns);
        drawLightBox(g2d, 460, 315, ew);
        drawLightBox(g2d, 300, 465, ew);
    }

    private void drawLightBox(Graphics2D g2d, int x, int y, LightState state) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x, y, 20, 20);

        if (state == LightState.GREEN) g2d.setColor(Color.GREEN);
        else if (state == LightState.YELLOW) g2d.setColor(Color.YELLOW);
        else g2d.setColor(Color.RED);

        g2d.fillOval(x + 3, y + 3, 14, 14);
    }
}