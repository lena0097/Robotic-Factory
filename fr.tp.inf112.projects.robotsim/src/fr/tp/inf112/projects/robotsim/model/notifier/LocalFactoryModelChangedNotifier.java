package fr.tp.inf112.projects.robotsim.model.notifier;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.robotsim.model.Factory;

public class LocalFactoryModelChangedNotifier implements FactoryModelChangedNotifier {

    private final Factory factory;

    public LocalFactoryModelChangedNotifier(final Factory factory) {
        this.factory = factory;
    }

    @Override
    public void notifyObservers() {
        for (final Observer observer : factory.getObservers()) {
            observer.modelChanged();
        }
    }

    @Override
    public boolean addObserver(final Observer observer) {
        return factory.getObservers().add(observer);
    }

    @Override
    public boolean removeObserver(final Observer observer) {
        return factory.getObservers().remove(observer);
    }
}
