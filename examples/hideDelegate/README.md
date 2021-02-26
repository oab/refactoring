Note in `plain.abs` that we have

```
interface ClientI {
    Unit  enquire(Bool rem);
}
```

which when set to `False` or `True` decides
`d = new Dept()` or `d = new Dept()` respectively in Client's implementation
of `Unit enquire(Bool rem)`.

For the snippet in the `Client` implementation of `Unit enquire(Bool rem)`
in `before.abs`.

```
	d = p.getDept();
	m = d.getManager();
``` 
We have the situation of calls `Client -> Person; Client -> Department`, read
this as "a Client object calls a Person object and then ..."


we expect that in the refactored code 
```
m = p.getManager()
```
where we will have the situation  of calls `Client -> (Person -> Department)`,
read this as "a Client object calls a Person object which calls a Department object".
Here `Department` reside in the same cog as `Client` when the call in `Client`
is done through `enquire(False)`. 

We expect the following can occur: A `Client` object in cog X calls a `Person` object in 
cog Y and the `Person` object becomes the active object in cog Y. The active object `Person` 
in cog Y calls the `Department` object which resides in cog X and which can only become 
active in cog X when the `Client` object in cog X completes its call to  the `Person` object 
in cog Y; we have a deadlock.

If we adopt the notation (similar to the formal one) of 
```
cog Name = {active_obj[what it is doing]|suspended_obj1,suspended_obj2,...}`  
```
we can describe the deadlock situation as

```
cog X = {Client[waiting for call to Person to return]|Department}
cog Y = {Person[waiting for call to Department to return]|...}
```

=== `interleaved.abs`

This example contains an unrelated call (here: `skip`) between both calls. While we may not directly
**identify** this instance (it could be normalized), Volker thinks the API should definitely be able
to handle this case gracefully.

=== `def-used.abs`

This example still needs the first argument, so the refactoring has to be careful about removing
the declaration. Note that the target variable is named `t` here, not `d`.

