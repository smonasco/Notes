package org.shannon.notes.controllers.test;

import lombok.val;
import org.assertj.core.util.Lists;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.shannon.notes.controllers.NoteController;
import org.shannon.notes.entities.Note;
import org.shannon.notes.repositories.NoteRepository;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class NoteControllerTest {
    @Rule
    public final JUnitRuleMockery mockery = new JUnitRuleMockery();
    private final NoteRepository repository = mockery.mock(NoteRepository.class);
    private final NoteController controller = new NoteController(repository);

    @Test
    public void givenId_whenPost_thenBadRequest() {
        // Given: note with id and a repository
        val note = new Note(1L, "Those who are free of resentful thoughts surely find peace.");

        // When: post
        val response = controller.post(note);

        // Then: the response is a bad request
        assertEquals("Should be Bad Request", HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void givenNoteAndBustedRepository_whenPost_thenInternalError() {
        // Given: note and a busted repository
        val note = new Note(null, "A man is not called wise because he talks and talks again; but is he peaceful, loving and fearless then he is in truth called wise.");
        mockery.checking(new Expectations() {{
            oneOf(repository).save(note);
                will(returnValue(null));
        }});

        // When: post
        val response = controller.post(note);

        // Then: the response is Internal Error
        assertEquals("Should be Internal Error", HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    public void givenNoteAndValidRepositoryResponse_whenPost_thenSavedNote() {
        // Given: a valid note and a good repository response
        val givenNote = new Note(null, "Even as a solid rock is unshaken by the wind, so are the wise unshaken by praise or blame.");
        val savedNote = givenNote.withId(1L);
        mockery.checking(new Expectations() {{
            oneOf(repository).save(givenNote);
                will(returnValue(savedNote));
        }});

        // When: post
        val response = controller.post(givenNote);

        // Then: we get the saved note
        assertEquals("Should be ok", HttpStatus.OK, response.getStatusCode());
        assertEquals("Response should be the correct note", savedNote, response.getBody());
    }

    @Test
    public void givenNoID_whenPut_thenBadRequest() {
        // Given: no ID
        val givenNote = new Note(null, "To conquer oneself is a greater task than conquering others.");

        // When: put
        val response = controller.put(null, givenNote);

        // Then: response is bad request
        assertEquals("Must have id", HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // I'm saying that I am white box testing and know that if null is returned then the post tests this
    @Test
    public void givenValidNote_whenPut_thenGetGiven() {
        // Given: note that's been saved
        val id = 1L;
        val givenNote = new Note(id, "The only real failure in life is not to be true to the best one knows.");
        mockery.checking(new Expectations() {{
            allowing(repository).findByID(id);
                will(returnValue(Optional.of(givenNote)));
            oneOf(repository).delete(givenNote);
            oneOf(repository).save(givenNote);
                will(returnValue(givenNote));
        }});

        // When: put
        val response = controller.put(id, givenNote);

        // Then: response is the given
        assertEquals("Should be ok", HttpStatus.OK, response.getStatusCode());
        assertEquals("Should be what's given", givenNote, response.getBody());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void givenNoID_whenDelete_thenBadRequest() {
        // Given: no id
        Long id = null;

        // When: delete
        val response = controller.delete(id);

        // Then: response is bad request
        assertEquals("bad request", HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void givenSomeID_whenDelete_thenItIsDeleted() {
        // Given: some id
        val id = 1L;
        mockery.checking(new Expectations() {{
            oneOf(repository).delete(id);
        }});

        // When: delete
        val response = controller.delete(id);

        // Then: we get no content
        assertEquals("Should be no content", HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void givenNoID_whenGet_thenBadRequest() {
        // Given: no id
        Long id = null;

        // When: delete
        val response = controller.get(id);

        // Then: response is bad request
        assertEquals("bad request", HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void givenSomeID_whenGet_thenItIsReturned() {
        // Given: some id
        val id = 1L;
        val note = new Note(id, "A dog is not considered a good dog because he is a good barker. A man is not considered a good man because he is a good talker.");
        mockery.checking(new Expectations() {{
            oneOf(repository).findByID(id);
                will(returnValue(Optional.of(note)));
        }});

        // When: delete
        val response = controller.get(id);

        // Then: we get no content
        assertEquals("Should be ok", HttpStatus.OK, response.getStatusCode());
        assertEquals("Should get what I request", note, response.getBody());
    }

    private List<Note> findableNotes() {
        return Arrays.asList(
                new Note(1L, "Every morning we are born again. What we do today is what matters most.")
                , new Note(2L, "Nothing is permanent.")
                , new Note(3L, "A jug fills drop by drop.")
        );
    }

    @Test
    public void getAll() {
        // Given: some notes findable
        val notes = findableNotes();
        mockery.checking(new Expectations() {{
            oneOf(repository).findAll();
                will(returnValue(notes));
        }});

        // When: get(null)
        val result = controller.get((String)null);

        // Then: we get all the notes
        assertEquals("Should get what is returned", notes, result);
    }

    @Test
    public void search() {
        // Given: some notes findable and a queryString
        val notes = findableNotes();
        val queryString = "morning AND born";
        mockery.checking(new Expectations() {{
            oneOf(repository).search(queryString, Integer.MAX_VALUE);
                will(returnValue(Stream.of(notes.get(0))));
        }});

        // When: get(queryString)
        val result = Lists.newArrayList(controller.get(queryString));

        // Then: we get the matching note
        assertEquals("Should only have the one note", 1, result.size());
        assertEquals("The only note should be the expected note", notes.get(0), result.get(0));
    }
}
