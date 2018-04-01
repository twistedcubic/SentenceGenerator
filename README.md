## Sentence Generator

A sentence generator using the probabilities of [universal dependency relations](http://universaldependencies.org/).

---

### Examples of auto-generated sentences:

* yet that has more aboriginal toiletry.
* we has abnormal blameworthiness.
* screechy wallpaper brook Iraq.
* Bush also roughhouse roost.
* feckless comic input along dolorous wrongfulness.
* limp tactician Al recommence  salutary mid crackpot.
* would that seine ruminate these chamberlain.
* self-discipline sorrowfulness undefended save US.

###Build
The project can be built with target "run" via `ant run`.

###Inspiration

I developed this during a period of obsession over universal dependencies as a means of building language models, I wanted to see how capable they are to create coherent sentences. It is a Markov chain based algorithm using exclusively the two sets conditional probabilities, one for each pair of parts-of-speech (pos) and another for the possible relations originating from a given pos, to generate sentences.

The code heavily takes advantage of the object-oriented design of Java, and encapsulates each pos and grammar relation in classes and enums for modularity and logic clarity.