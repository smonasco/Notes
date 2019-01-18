package org.shannon.notes.entities;

import lombok.*;
import lombok.experimental.Wither;
import org.apache.lucene.document.Document;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Wither
    Long id;

    String body;

    public static Note fromDocument(Document doc) {
        return new Note(Long.decode(doc.get("id")), doc.get("body"));
    }
}
