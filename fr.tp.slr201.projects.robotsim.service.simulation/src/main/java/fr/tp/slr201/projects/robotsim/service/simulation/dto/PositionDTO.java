package fr.tp.slr201.projects.robotsim.service.simulation.dto;

public class PositionDTO {
    private int x;
    private int y;

    public PositionDTO() {}

    public PositionDTO(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
