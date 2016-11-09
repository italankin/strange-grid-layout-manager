# StrangeGridLayoutManager #

This repository contains an implementation of [RecyclerView.LayoutManager](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.LayoutManager.html) which arranges child views in strange way.

![Screen 1](/art/screen01.jpg)
![Screen 2](/art/screen02.jpg)
![Screen 3](/art/screen03.jpg)

## Implementation details ##

- each child has fixed size (square)
- various columns count for each row
- smooth scroll and scroll to position
- scroll position and state saving
- has adaptive mode, in which manager will calculate column counts based on minumum child size
- `ItemDecorator`s, which add extra offsets for the child views are not supported

## TODOs ##

- add support for scroll bars
- implement support for predictive animations

# License #

    Copyright 2016 Igor Talankin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.