package android.os;

// TESTING MOCK: runs posted runnables inline so handler-based logic (e.g. IdleTimerManagement)
// executes synchronously on the test thread
public class Handler {
    public Handler() {}

    public boolean post(Runnable r) {
        r.run();
        return true;
    }
}
