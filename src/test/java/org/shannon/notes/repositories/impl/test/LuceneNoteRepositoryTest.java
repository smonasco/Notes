package org.shannon.notes.repositories.impl.test;

import lombok.Cleanup;
import lombok.val;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shannon.notes.entities.Note;
import org.shannon.notes.repositories.impl.LuceneNoteRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class LuceneNoteRepositoryTest {

    private LuceneNoteRepository repository;

    @Before
    public void setUp() throws IOException {
        Directory directory = new MMapDirectory(Files.createTempDirectory("test-index"));
        repository = new LuceneNoteRepository(directory);
    }

    @After
    public void tearDown() throws IOException {
        repository.close();
    }

    @Test
    public void saveNoID() {
        // Given: no ID and a string
        val givenNote = new Note(null, "The tongue like a sharp knife… Kills without drawing blood.");

        // When: save
        val savedNote = repository.save(givenNote);

        // Then: the saved Note has a generated id and is findable
        assertNotNull("Should have generated ID", savedNote.getId());
        val retrievedNote = repository.findByID(savedNote.getId());
        assertTrue("Should be retrieved", retrievedNote.isPresent());
        assertEquals("SavedNote should be retrieved", savedNote, retrievedNote.get());
        assertEquals("SavedNote should have the same body as the Given Note", givenNote.getBody(), savedNote.getBody());
    }

    @Test
    public void saveID() {
        // Given: some already saved note
        val givenNote = repository.save(new Note(null, "Pain is certain; suffering is optional."));

        // When: save
        val savedNote = repository.save(givenNote);

        // Then:
        //              * the saved note is the given note
        //              * the saved note is findable
        assertEquals("saved note should be given note", givenNote, savedNote);
        val foundNote = repository.findByID(givenNote.getId());
        assertTrue(foundNote.isPresent());
        assertEquals("saved note is findable", savedNote, foundNote.get());
    }

    @Test
    public void search() {
        // Given: an few indexed notes
        val note1 = repository.save(new Note(null, "Three things cannot be long hidden: the sun, the moon and the truth."));
        val note2 = repository.save(new Note(null, "The only real failure in life is not to be true to the best one knows."));
        repository.save(new Note(null, "Purity or impurity depends on oneself. No one can purify another."));

        // When: search for part of 2 notes
        val findings = repository.search("moon sun best", 10)
                .collect(Collectors.toList());

        // Then: only those notes are found
        assertEquals("Should have 2 notes found", 2, findings.size());
        assertEquals("note1 matched on 2 terms so should be first", note1.getId(), findings.get(0).getId());
        assertEquals("and we should have note2 as well", note2.getId(), findings.get(1).getId());
    }

    private static void indexSomeNotes(int indexCount, LuceneNoteRepository myRepository) {
        IntStream.range(0, indexCount)
                .forEach(i -> myRepository.save(new Note(null, Integer.toString(i))));
    }

    @Test
    public void dataPersists() throws IOException {
        // Given: an index with some data
        val tempDir = Files.createTempDirectory("another-index");
        val oldRepository = new LuceneNoteRepository(new MMapDirectory(tempDir));
        indexSomeNotes(100, oldRepository);
        val previousNote = oldRepository.save(new Note(null, "If you light a lamp for somebody, it will also brighten your path."));
        oldRepository.close();

        // When: the index is opened again
        @Cleanup
        val newRepository = new LuceneNoteRepository(new MMapDirectory(tempDir));

        // Then:
        //          * the next note has the next id
        //          * the old note is findable
        val newNote = newRepository.save(new Note(null, "You will not be punished for your anger, you will be punished by your anger."));
        assertEquals("the new note should have an id one more than the old one", previousNote.getId() + 1, (long)(newNote.getId()));
        assertTrue("the old note is findable", newRepository.findByID(previousNote.getId()).isPresent());
    }

    @Test
    public void delete() {
        // Given: an indexed note
        val note = repository.save(new Note(null, "It is better to travel well than to arrive."));

        // When: delete
        repository.delete(note);

        // Then: the note is lost
        assertFalse("note can't be found", repository.findByID(note.getId()).isPresent());
    }

    @Test
    public void findAll() {
        // Given: a few indexed docs
        val indexCount = 100;
        indexSomeNotes(indexCount, repository);

        // When: findAll
        val results = repository.findAll();

        // Then: all are found
        val countFound = StreamSupport.stream(results.spliterator(), false)
                .count();
        assertEquals("Should have the same number of found notes as indexed notes", indexCount, countFound);
    }
}
