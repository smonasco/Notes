package org.shannon.notes.controllers;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.shannon.notes.Main;
import org.shannon.notes.entities.Note;
import org.shannon.notes.repositories.NoteRepository;
import org.shannon.notes.repositories.impl.LuceneNoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Controls all CRUD and search actions on notes
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notes")
public final class NoteController {
    private final @NonNull NoteRepository repository;

    /**
     * Some basic constructor that defaults the repository used to one constructed in Main.main()
     *
     * @throws IOException          An exception thrown by LuceneNoteRepository's constructor
     */
    public NoteController() throws IOException {
        repository = new LuceneNoteRepository(Main.directory);
    }

    /**
     * Utility function to construct a Bad Request response
     *
     * @param hint          Some hint for the user
     * @return              The Bad Request response
     */
    private ResponseEntity<?> badRequest(String hint) {
        val response = ResponseEntity.badRequest();
        response.body(hint);
        return response.build();
    }

    /**
     * Wrapper around saving a Note
     *
     * @param note          The note to save
     * @return              An appropriate response given how the save went
     */
    private ResponseEntity save(Note note) {
        val savedNote = repository.save(note);

        if (savedNote == null) {
            val response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
            response.body("Something went wrong and could not save your note.");
            return response.build();
        } else {
            return ResponseEntity.ok(savedNote);
        }
    }

    /**
     * Post a note and get back the id (and rest of the note)
     *
     * @param note      Some note to post
     * @return          The note as it is saved (with id)
     */
    @PostMapping
    public ResponseEntity<?> post(@RequestBody Note note) {
        if (note.getId() != null) { return badRequest("It is invalid to supply ID."); }
        else { return save(note); }
    }

    /**
     * Utility function to return an appropriate response if the id is not present.
     *
     * @param id                The possibly missing id
     * @param happyPath         What to do if the id is present
     * @return                  The appropriate response.
     */
    private ResponseEntity<?> requireID(Long id, Supplier<ResponseEntity<?>> happyPath) {
        if (id == null) { return badRequest("We need an id in the URL."); }
        else { return happyPath.get(); }
    }

    /**
     * Put is idempotent which means repeated uses should not have different end effects.
     *
     * Note: here since choosing the id is the purview of the database, we do not allow Put
     *      except where the note is already in the system (Updates only)
     *
     * @param id                The id of the Note
     * @param givenNote         Some note to use to overwrite the current note
     * @return                  An appropriate response
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> put(@PathVariable Long id, @RequestBody Note givenNote) {
        return requireID(id, () -> {
            if (givenNote.getId() != null && !givenNote.getId().equals(id)) {
                return badRequest("ID in the note must match ID in the URL.");
            }
            givenNote.setId(id);
            val currentNote = repository.findByID(id);
            if (currentNote.isPresent()) {
                repository.delete(currentNote.get());
                return save(givenNote);
            } else {
                return badRequest("Cannot supply your own id.");
            }
        });
    }

    /**
     * Delete a note by its id.
     *
     * @param id        The id of the note to delete
     * @return          An appropriate response (NoContent if it succeeds or BadRequest if malformed)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return requireID(id, () -> {
            repository.delete(id);
            return ResponseEntity.noContent().build();
        });
    }

    /**
     * Get the note by id
     *
     * @param id        The id of the note to get
     * @return          The note if found or an appropriate error
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return requireID(id, () -> repository.findByID(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    /**
     * Get either all or the results of a query.
     *
     * Query syntax is lucene.
     *
     * @param queryString   Lucene query string
     * @return              What was found.
     */
    @GetMapping
    public List<Note> get(@RequestParam(value="query", required=false) String queryString) {
        return Optional.ofNullable(queryString)
                .map(s -> repository.search(s, Integer.MAX_VALUE).collect(Collectors.toList()))
                .orElseGet(() -> Lists.newArrayList(repository.findAll()));
    }
}
