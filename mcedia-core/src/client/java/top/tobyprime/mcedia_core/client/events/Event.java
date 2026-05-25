package top.tobyprime.mcedia_core.client.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class Event<TArg> {
    public static final Logger LOGGER = LoggerFactory.getLogger(Event.class);
    public HashSet<EventHandler<TArg>> handlers = new HashSet<EventHandler<TArg>>();
    public String name;

    public Event() {
        this.name = "未知事件";
    }

    public Event(String name) {
        this.name = name;
    }

    public void addHandler(EventHandler<TArg> handler) {
        handlers.add(handler);
    }

    public void removeHandler(EventHandler<TArg> handler) {
        handlers.remove(handler);
    }

    public void handle(TArg arg) {
        for (EventHandler<TArg> handler : handlers) {
            try {
                handler.handle(arg);
            } catch (Exception e) {
                LOGGER.error("处理事件 {} 时发生异常", name, e);
            }
        }
    }

    @FunctionalInterface
    public interface EventHandler<TArg> {
        void handle(TArg arg);
    }
}
