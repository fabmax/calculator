# Calculator
An Android calculator app with my own OpenGL-based UI. I did this as a little
experiment to see how fully animated orientation changes and true 3D layouts could
work. However I'm not a UX guy so this might be complete rubbish :)

I tried to make this as Material-Design-ish as possible, so the app looks pretty
standard at first. However, rotate the screen or hit the view button and you should
see the difference...

For the OpenGL rendering I'm using my own [LightGL](https://github.com/fabmax/LightGL)
engine. If you clone this repo don't forget to make a ``git submodule init`` and
``update`` to get the LightGL code.

The app itself is written in [Kotlin](https://kotlinlang.org), which offers super-cool
language features. E.g. I use the
[Builder pattern](https://kotlinlang.org/docs/reference/type-safe-builders.html)
to define my UI layouts. For example a simple layout looks like this:
``` kotlin
val exampleLayout = layout(context) {
    bounds(rw(-0.5f), rh(-0.5f), rw(1.0f), rh(1.0f))

    button {
        init {
            color = Color.RED
            text = "Button 1"
            onClickListener = { -> Log.d("Layout", "Button 1 pressed") }
        }
        port {
            bounds(dp(32f), dp(32f), rw(1f) - dp(64f), rh(.5f) - dp(48f))
        }
        land {
            bounds(dp(32f), dp(32f), rw(.5f) - dp(48f), rh(1f) - dp(64f))
        }
    }

    button {
        init {
            color = Color.GREEN
            text = "Button 2"
            onClickListener = { -> Log.d("Layout", "Button 2 pressed") }
        }
        port {
            bounds(dp(32f), rh(.5f) + dp(16f), rw(1f) - dp(64f), rh(.5f) - dp(48f))
        }
        land {
            bounds(rw(.5f) + dp(16f), dp(32f), rw(.5f) - dp(48f), rh(1f) - dp(64f))
        }
    }
}
```
You could actually paste this code into MainActivity.kt and it would work showing
a red and a green button with a nice layout transition between portrait and
landscape orientation. Sweet!

But wait what's happening here? First of all a top-level layout is created which
contains the buttons. The first bounds() statement may look a little odd but it simply
makes the layout fill the screen: The arguments of bounds() are (x, y, width, height).
Initially the coordinate origin is in the center of the screen, ``rw(.5f)`` and ``rh(.5f)``
return a ``SizeSpec`` which translate to a relative width and height of 50%, hence the
layout's upper left corner is moved to the upper left corner of the screen. Width
and height are set to 100%.

Once the layout is created we can add the buttons to it. Within the ``init { }`` blocks
the button properties are set. Finally, within the ``port { }`` and ``land { }`` blocks
the button bounds for portrait and landscape orientations are defined. Bounds are
always relative to the next higher parent. Besides rw() and rh() for relative sizes
there is also dp() which defines a pixel perfect absolute size with the usual Android
dp units. Moreover, ``SizeSpec``s can be chained together with + and -.

There is also a second bounds() method with two additional parameters for z and depth.
Positive z values will make UI elements appear nearer, negative values farther away.
The default z value used by the 4 parameter version is 0. Also keep in mind that,
because of the perspective camera, layouts are only pixel-perfect at z = 0.

To see how all this works syntax-wise take a look into the Kotlin docs.

## Limitations / Issues:
This is only a proof-of-concept I made to see if and how animated layout changes can be
done, so there are a few pretty major ones:
* Currently the only available UI elements are buttons and a very specific calculator
  text panel.
* There are no layout managers, component bounds always have to be explicitly specified.
  The chainable rw(), rh() and dp() methods are surprisingly flexible but obviously they
  can't replace a true layout manager like ``RelativeLayout``.
* The code is not really optimized for performance. The engine itself is pretty fast
  (though written entirely in Java). However the way the UI elements are composed isn't
  optimized for draw calls or anything. Moreover especially the shadow computation is
  pretty expensive so this will eat up your battery in no-time (and also doesn't work
  very well on slower devices).
* Touch input is very basic.
* The system screen orientation is locked to portrait, otherwise the orientation change
  animation would be interrupted by the system screen orientation change.
* I haven't really cared for the integration of Android ressources (strings, dimens,
  etc.) but it should be pretty straight-forward to do so.
* The code is a bit of a mess - I'm still new to Kotlin and still working on getting
  a feeling for good style.


## License:
```
Copyright 2016 Max Thiele

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```