public class LamportRequest {
    private int clock;
    private String process;
    private int id;

    public LamportRequest(int clock, String process, int id){
        this.clock = clock;
        this.process = process;
        this.id = id;
    }

    public int getClock() {
        return clock;
    }

    public String getProcess() {
        return process;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "LamportRequest{" +
                "clock=" + clock +
                ", process='" + process + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LamportRequest that = (LamportRequest) o;
        return clock == that.clock &&
                id == that.id &&
                process.equals(that.process);
    }

    @Override
    public int hashCode() {
        return id;
    }
}
