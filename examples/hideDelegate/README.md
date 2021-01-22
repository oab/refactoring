Note in `before.abs` that we have

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

We expect the following: A `Client` object calls `Person` object in cog X and the
`Person` object becomes the active object in cog X. The active `Person` object which 
may not reside in cog X calls the `Department` object which resides in cog X,
same as `Client`, and can only become active when `Client` completes its call;
we should have a deadlock.  
