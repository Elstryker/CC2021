public class MyPair<F,S> {
    F first;
    S second;

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

    public void setSecond(S second) {
        this.second = second;
    }

    public void setFirst(F first) {
        this.first = first;
    }
}
