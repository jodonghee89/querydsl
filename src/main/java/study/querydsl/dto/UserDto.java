package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

	public UserDto(String name, int age) {
		this.name = name;
		this.age = age;
	}

	private String name;
	private int age;
}
