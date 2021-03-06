/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef SHARE_GC_Z_ZPAGECACHE_INLINE_HPP
#define SHARE_GC_Z_ZPAGECACHE_INLINE_HPP

#include "gc/z/zList.inline.hpp"
#include "gc/z/zPageCache.hpp"
#include "gc/z/zValue.inline.hpp"

inline size_t ZPageCache::available() const {
  return _available;
}

template <typename Closure>
inline void ZPageCache::pages_do(Closure* cl) const {
  // Small
  ZPerNUMAConstIterator<ZList<ZPage> > iter_numa(&_small);
  for (const ZList<ZPage>* list; iter_numa.next(&list);) {
    ZListIterator<ZPage> iter_small(list);
    for (ZPage* page; iter_small.next(&page);) {
      cl->do_page(page);
    }
  }

  // Medium
  ZListIterator<ZPage> iter_medium(&_medium);
  for (ZPage* page; iter_medium.next(&page);) {
    cl->do_page(page);
  }

  // Large
  ZListIterator<ZPage> iter_large(&_large);
  for (ZPage* page; iter_large.next(&page);) {
    cl->do_page(page);
  }
}

#endif // SHARE_GC_Z_ZPAGECACHE_INLINE_HPP
