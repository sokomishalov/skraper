/*
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.util;

import org.junit.jupiter.api.Test;
import ru.sokomishalov.skraper.Skraper;
import ru.sokomishalov.skraper.model.PageInfo;
import ru.sokomishalov.skraper.model.Post;
import ru.sokomishalov.skraper.provider.pikabu.PikabuSkraper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.sokomishalov.skraper.util.JavaInterop.callBlocking;
import static ru.sokomishalov.skraper.util.JavaInterop.limitedFlow;

public class JavaInteropTest {
    private final Skraper checkSkraper = new PikabuSkraper();
    private final String checkPath = "/best";

    @Test
    void limitedFlowWorks() {
        List<Post> posts = limitedFlow(checkSkraper.getPosts(checkPath), 10);
        assertFalse(posts.isEmpty());
    }

    @Test
    void callBlockingWorks() {
        PageInfo info = callBlocking(coroutine -> checkSkraper.getPageInfo(checkPath, coroutine));
        System.out.println(info);
    }
}
