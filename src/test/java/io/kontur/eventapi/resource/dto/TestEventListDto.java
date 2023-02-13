package io.kontur.eventapi.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEventListDto implements Serializable {

	@Serial
	private static final long serialVersionUID = -602376647576312525L;

	private List<TestEventDto> data;
	private TestPageMetadata pageMetadata;

	@Serial
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		data = (List<TestEventDto>) in.readObject();
		pageMetadata = (TestPageMetadata) in.readObject();
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(data);
		out.writeObject(pageMetadata);
	}
}
