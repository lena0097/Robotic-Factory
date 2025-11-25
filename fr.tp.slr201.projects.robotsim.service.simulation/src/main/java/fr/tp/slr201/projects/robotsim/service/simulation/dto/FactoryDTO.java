package fr.tp.slr201.projects.robotsim.service.simulation.dto;

import java.util.ArrayList;
import java.util.List;

public class FactoryDTO {
    private String id;
    private int width;
    private int height;
    private List<ComponentDTO> components = new ArrayList<>();

    public FactoryDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public List<ComponentDTO> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentDTO> components) {
        this.components = components;
    }
}
