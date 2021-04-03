public class MyPair<F,S> {
    private final F first;
    private final S second;

    public MyPair(F fir, S sec) {
        first = fir;
        second = sec;
    }

    public S getSecond() {
        return second;
    }

    public F getFirst() {
        return first;
    }
}
