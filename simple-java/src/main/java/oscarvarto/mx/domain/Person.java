package oscarvarto.mx.domain;

import java.util.Objects;

public final class Person {

    private final String name;
    private final int age;

    public static final int MAX_AGE = 130;

    public Person(String name, int age) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty or contain only white space");
        }
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (age > MAX_AGE) {
            throw new IllegalArgumentException("Age cannot be bigger than " + MAX_AGE + " years");
        }
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person other = (Person) o;
        return age == other.age && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person(name=" + name + ", age=" + age + ")";
    }
}
