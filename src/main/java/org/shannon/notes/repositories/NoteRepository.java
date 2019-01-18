package org.shannon.notes.repositories;

import org.shannon.notes.entities.Note;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface NoteRepository extends CrudRepository<Note, Long> {
    /**
     * Get the Note by its id or return Optional.Empty()
     *
     * @param id        The id of the note to find
     * @return          The Note if found
     */
    Optional<Note> findByID(long id);

    /**
     * Searches using the queryString for up to count notes.
     *
     * @param queryString       Some lucene query string
     * @param count             Max number of notes to find
     * @return                  A Stream of the Notes found
     */
    Stream<Note> search(String queryString, int count);
}
