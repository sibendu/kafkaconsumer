package coms;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @Getter @Setter
public class Order implements Serializable {

	private String orderNum;
	private Long delay;
	private Long value;
	private String errorType;
	
}
