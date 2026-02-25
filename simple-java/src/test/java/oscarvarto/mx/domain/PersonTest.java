package oscarvarto.mx.domain;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PersonTest {

    @Test
    public void nameCannotBeEmptyTest() {
        assertThatThrownBy(() -> new Person("", 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be empty or contain only white space");
    }

    @Test
    public void nameCannotBeBlankTest() {
        assertThatThrownBy(() -> new Person("  ", 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name cannot be empty or contain only white space");
    }

    @Test
    public void ageCannotBeNegativeTest() {
        assertThatThrownBy(() -> new Person("Alice", -3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Age cannot be negative");
    }

    @Test
    public void ageCannotBeGreaterThanMaxAgeTest() {
        assertThatThrownBy(() -> new Person("Alice", 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Age cannot be bigger than 130 years");
    }

    @Test
    public void validPersonTest() {
        Person person = new Person("Paco de Lucía", 66);
        assertThat(person.getName()).isEqualTo("Paco de Lucía");
        assertThat(person.getAge()).isEqualTo(66);
    }
}
