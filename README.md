# Looping Layout

The Looping Layout Project is a LayoutManager for the Android RecyclerView. With this project you 
can add looping/circular/endless functionality to a RecyclerView.

Unlike other solutions for creating a looping recycler, which involve modifying the Adapter, this
project isolates all logic inside the LayoutManager. This allows your Adapter to be reused in other 
non-looping layouts, and it better conforms to the MVC-like architecture provided by the RecyclerView.

This project was original created and is maintained by [Beka Westberg][linked-in] (BeksOmega).

It lives at [https://github.com/BeksOmega/looping-layout].

## :star2: Setup 

Add the dependency to your build.gradle file.
```groovy
dependencies {
    implementation 'com.github.beksomega:loopinglayout:0.1.0'
}
```

Apply the layout manager to your RecyclerView.

Kotlin:
```kotlin
class MyActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_activity)

        viewManager = LoopingLayoutManager(
                this,                           // Pass the context.
                LoopingLayoutManager.VERTICAL,  // Pass the orientation. Vertical by default.
                false                           // Pass whether the views are laid out in reverse.
                                                // False by default.
        )
        viewAdapter = MyAdapter(myDataset)

        recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }
    // ...
}
```

Java:
```java
public class MyActivity extends Activity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activity);
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        recyclerView.setHasFixedSize(true);

        layoutManager = new LoopingLayoutManager(
                this,                           // Pass the context.
                LoopingLayoutManager.VERTICAL,  // Pass the orientation. Vertical by default.
                false                           // Pass whether the views are laid out in reverse.
                                                // False by default.
        );
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new MyAdapter(myDataset);
        recyclerView.setAdapter(mAdapter);
    }
    // ...
}
```

Example code was modified from [https://developer.android.com/guide/topics/ui/layout/recyclerview]. See
that for more information about RecyclerViews.

## :sparkles: Features 

Current:
 * Vertical and Horizontal Orientations.
 * LTR and RTL support.
 * ReverseLayout support for both orientations, as well as LTR, and RTL.
    
Future:
 * `notifyDataSetChanged` support.
 * Snap Helper support.
 * Public functions for finding items and positions.
 * Public functions for scrolling.
 * Drag and Drop support?

## :page_with_curl: License 

Looping Layout is licenced under the [Apache 2.0 License][apache].

This means it can be used for commercial, public, or private use. You are also free to modify
and/or distribute the software. You simply need to maintain the copyright included in each file,
and include the license when distributing the library.

## :green_heart: Contributing 

Contributions are always welcome! Contributing code, writing bug reports,
and commenting on feature requests are all super important to this project.

For more info about types of contributions and ways to contribute, please
see the [contribution guide][contributing].

## :question: Support 

If you think you have found a bug definitely report it using the [issue template][issue-template]! Just be sure
to search for for duplicate issues before reporting, as someone else may have already come across
your problem.

If you have any questions about the project please feel free to message [bekawestberg@gmail.com] with
the subject line "Looping Layout". All questions are welcome, don't be shy! Just try to include
as much helpful information as possible =)

Currently there is no mailing list, but if you would like one please message [bekawestberg@gmail.com]
about that as well!

[apache]: https://www.apache.org/licenses/LICENSE-2.0
[contributing]: https://github.com/BeksOmega/looping-layout/blob/master/.github/CONTRIBUTING.md
[issue-template]: https://github.com/BeksOmega/looping-layout/issues/new/choose
[linked-in]: https://www.linkedin.com/in/beka-westberg/
