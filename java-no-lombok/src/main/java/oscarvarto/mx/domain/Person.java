package oscarvarto.mx.domain;

import fj.data.NonEmptyList;
import fj.data.Validation;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import oscarvarto.mx.qual.ErrorMsg;

import static fj.Semigroup.nonEmptyListSemigroup;
import static oscarvarto.mx.validation.ValidationOps.check;

public final class Person {
    private final String name;
    private final int age;

    public static final int MAX_AGE = 130;
    public static final @ErrorMsg String NAME_EMPTY_OR_WHITESPACE_ERROR_MSG =
            "Name cannot be empty or contain only white space";
    public static final @ErrorMsg String NEGATIVE_AGE_ERROR_MSG = "Age cannot be negative";

    @SuppressWarnings("assignment") // string concatenation is not a literal for @QualifierForLiterals
    public static final @ErrorMsg String MAX_AGE_ERROR_MSG = "Age cannot be bigger than " + MAX_AGE + " years";

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public static Validation<NonEmptyList<@ErrorMsg String>, Person> of(@NonNull String name, int age) {
        var validatedName = check(!name.isBlank(), NAME_EMPTY_OR_WHITESPACE_ERROR_MSG, name);
        var validatedMinAge = check(age >= 0, NEGATIVE_AGE_ERROR_MSG, age);
        var validatedMaxAge = check(age <= MAX_AGE, MAX_AGE_ERROR_MSG, age);
        return validatedName.accumulate(
                nonEmptyListSemigroup(), validatedMinAge, validatedMaxAge, (n, a1, a2) -> new Person(n, a1));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Person other)) return false;
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
