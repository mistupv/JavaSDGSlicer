class Test {

    int a = 10;

    void f() {
        a += 20;
    }

    public static void main(String[] args) {
        Test t = new Test();
        Test[] array = new Test[] { t };
        TestContainer tc = new TestContainer();
        (t).f();
        array[0].f();
        (true ? t : array[0]).f();
        tc.test.f();
        System.out.println(t.a + array[0].a + tc.test.a);
    }
}

class TestContainer {

    Test test = new Test();
}

