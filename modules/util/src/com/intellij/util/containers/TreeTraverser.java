/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.containers;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;
import com.intellij.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;

/**
 * A pruned version of com.google.common.collect.TreeTraverser.
 *
 * Views elements of a type {@code T} as nodes in a tree, and provides methods to traverse the trees
 * induced by this traverser.
 *
 * <p>For example, the tree
 *
 * <pre>          {@code
 *          h
 *        / | \
 *       /  e  \
 *      d       g
 *     /|\      |
 *    / | \     f
 *   a  b  c       }</pre>
 *
 * <p>can be iterated over in preorder (hdabcegf), postorder (abcdefgh), or breadth-first order
 * (hdegabcf).
 *
 * <p>Null nodes are strictly forbidden.
 *
 * @author Louis Wasserman
 */
public class TreeTraverser<T> {

  protected final Function<T, ? extends Iterable<? extends T>> treeStructure;

  public TreeTraverser(Function<T, ? extends Iterable<? extends T>> provider) {
    treeStructure = provider;
  }

  /**
   * Returns the children of the specified node.  Must not contain null.
   */
  @NotNull
  public Iterable<? extends T> children(@NotNull T root) {
    Iterable<? extends T> result = treeStructure.fun(root);
    return result != null ? result : JBIterable.<T>empty();
  }

  public static abstract class TracingIt<T> extends JBIterator<T> {
    @Nullable
    public abstract T parent();

    @NotNull
    public abstract JBIterable<T> backtrace();
  }

  /**
   * Returns an unmodifiable iterable over the nodes in a tree structure, using pre-order
   * traversal. That is, each node's subtrees are traversed after the node itself is returned.
   *
   * <p>No guarantees are made about the behavior of the traversal when nodes change while
   * iteration is in progress or when the iterators generated by {@link #children} are advanced.
   */
  @NotNull
  public final JBIterable<T> preOrderDfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public TracingIt<T> iterator() {
        return new PreOrderIt((Iterable<T>)roots);
      }
    };
  }

  /**
   * @see #preOrderDfsTraversal(Iterable)
   */
  @NotNull
  public final JBIterable<T> preOrderDfsTraversal(@Nullable T root) {
    return preOrderDfsTraversal(ContainerUtil.createMaybeSingletonList(root));
  }


  /**
   * Returns an unmodifiable iterable over the nodes in a tree structure, using post-order
   * traversal. That is, each node's subtrees are traversed before the node itself is returned.
   * <p/>
   * <p>No guarantees are made about the behavior of the traversal when nodes change while
   * iteration is in progress or when the iterators generated by {@link #children} are advanced.
   */
  @NotNull
  public final JBIterable<T> postOrderDfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new PostOrderIt(roots);
      }
    };
  }

  /**
   * @see #postOrderDfsTraversal(Iterable)
   */
  @NotNull
  public final JBIterable<T> postOrderDfsTraversal(@Nullable T root) {
    return postOrderDfsTraversal(ContainerUtil.createMaybeSingletonList(root));
  }

  @NotNull
  public final JBIterable<T> leavesOnlyDfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new LeavesDfsIt(roots);
      }
    };
  }

  /**
   * Returns an unmodifiable iterable over the nodes in a tree structure, using breadth-first
   * traversal. That is, all the nodes of depth 0 are returned, then depth 1, then 2, and so on.
   * <p/>
   * <p>No guarantees are made about the behavior of the traversal when nodes change while
   * iteration is in progress or when the iterators generated by {@link #children} are advanced.
   */
  @NotNull
  public final JBIterable<T> bfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new BfsIterator(roots);
      }
    };
  }

  @NotNull
  public final JBIterable<T> bfsTraversal(@Nullable T root) {
    return bfsTraversal(ContainerUtil.createMaybeSingletonList(root));
  }

  @NotNull
  public final JBIterable<T> tracingBfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public TracingIt<T> iterator() {
        return new TracingBfsIt(roots);
      }
    };
  }

  @NotNull
  public final JBIterable<T> tracingBfsTraversal(@Nullable T root) {
    return tracingBfsTraversal(ContainerUtil.createMaybeSingletonList(root));
  }

  @NotNull
  public final JBIterable<T> leavesOnlyBfsTraversal(@NotNull final Iterable<? extends T> roots) {
    return new JBIterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new LeavesBfsIt(roots);
      }
    };
  }


  // -----------------------------------------------------------------------------
  // Iterators
  // -----------------------------------------------------------------------------

  private abstract static class DfsIt<T> extends TracingIt<T> {
    final ArrayDeque<Pair<T, Iterator<? extends T>>> stack = new ArrayDeque<Pair<T, Iterator<? extends T>>>();

    @Nullable
    public T parent() {
      Iterator<Pair<T, Iterator<? extends T>>> it = stack.descendingIterator();
      it.next();
      return it.hasNext() ? it.next().first : null;
    }

    @NotNull
    public JBIterable<T> backtrace() {
      return new JBIterable<Pair<T, Iterator<? extends T>>>() {
        @Override
        public Iterator<Pair<T, Iterator<? extends T>>> iterator() {
          Iterator<Pair<T, Iterator<? extends T>>> iterator = stack.descendingIterator();
          iterator.next();
          return iterator;
        }
      }.transform(Functions.<T>pairFirst()).filter(Condition.NOT_NULL);
    }
  }

  private final class PreOrderIt extends DfsIt<T> {

    int doneCount;

    PreOrderIt(@NotNull Iterable<T> roots) {
      Iterator<T> iterator = roots.iterator();
      if (iterator.hasNext()) {
        stack.addLast(Pair.<T, Iterator<? extends T>>create(null, iterator));
      }
    }

    @Override
    public T nextImpl() {
      if (stack.size() <= doneCount) return stop();
      Pair<T, Iterator<? extends T>> top;
      while (!(top = stack.getLast()).second.hasNext()) {
        stack.removeLast();
        doneCount--;
      }
      T result = top.second.next();
      if (!top.second.hasNext()) doneCount++;
      Iterator<? extends T> childItr = children(result).iterator();
      stack.addLast(Pair.<T, Iterator<? extends T>>create(result, childItr));
      if (!childItr.hasNext()) doneCount++;
      return result;
    }
  }

  private final class PostOrderIt extends DfsIt<T> {

    PostOrderIt(@NotNull Iterable<? extends T> roots) {
      for (T root : roots) {
        stack.addLast(Pair.<T, Iterator<? extends T>>create(root, children(root).iterator()));
      }
    }

    @Override
    public T nextImpl() {
      while (!stack.isEmpty()) {
        Pair<T, Iterator<? extends T>> top = stack.getLast();
        if (top.second.hasNext()) {
          T child = top.second.next();
          stack.addLast(Pair.<T, Iterator<? extends T>>create(child, children(child).iterator()));
        }
        else {
          stack.removeLast();
          return top.first;
        }
      }
      return stop();
    }
  }

  private final class LeavesDfsIt extends DfsIt<T> {

    LeavesDfsIt(@NotNull Iterable<? extends T> roots) {
      for (T root : roots) {
        Iterator<? extends T> childrenIt = children(root).iterator();
        stack.addLast(Pair.<T, Iterator<? extends T>>create(root, childrenIt.hasNext() ? childrenIt : null));
      }
    }

    @Override
    public T nextImpl() {
      while (!stack.isEmpty()) {
        Pair<T, Iterator<? extends T>> top = stack.getLast();
        if (top.second != null && top.second.hasNext()) {
          T child = top.second.next();
          Iterator<? extends T> childrenIt = children(child).iterator();
          if (childrenIt.hasNext()) {
            stack.addLast(Pair.<T, Iterator<? extends T>>create(child, childrenIt));
          }
          else {
            return child;
          }
        }
        else {
          stack.removeLast();
          if (top.second == null) return top.first;
        }
      }
      return stop();
    }
  }

  private final class BfsIterator extends JBIterator<T> {
    final ArrayDeque<T> queue = new ArrayDeque<T>();

    BfsIterator(@NotNull Iterable<? extends T> roots) {
      JBIterable.from(roots).addAllTo(queue);
    }

    @Override
    public T nextImpl() {
      if (queue.isEmpty()) return stop();
      T result = queue.remove();
      for (T t : children(result)) queue.add(t);
      return result;
    }
  }

  private final class LeavesBfsIt extends JBIterator<T> {
    final ArrayDeque<T> queue = new ArrayDeque<T>();

    LeavesBfsIt(@NotNull Iterable<? extends T> roots) {
      JBIterable.from(roots).addAllTo(queue);
    }

    @Override
    public T nextImpl() {
      while (!queue.isEmpty()) {
        T result = queue.remove();
        Iterator<? extends T> childrenIt = children(result).iterator();
        if (!childrenIt.hasNext()) return result;
        while (childrenIt.hasNext()) queue.add(childrenIt.next());
      }
      return stop();
    }
  }

  private final class TracingBfsIt extends TracingIt<T> {
    final ArrayDeque<T> queue = new ArrayDeque<T>();
    final Map<T, T> paths = ContainerUtil.newTroveMap(ContainerUtil.<T>identityStrategy());
    T cur;

    TracingBfsIt(@NotNull Iterable<? extends T> roots) {
      JBIterable.from(roots).addAllTo(queue);
    }

    @Override
    public T nextImpl() {
      if (queue.isEmpty()) return stop();
      T result = queue.remove();
      for (T t : children(result)) {
        if (paths.containsKey(t)) continue;
        queue.add(t);
        paths.put(t, result);
      }
      return cur = result;
    }

    @Override
    public T parent() {
      return paths.get(cur);
    }

    @NotNull
    @Override
    public JBIterable<T> backtrace() {
      final T first = cur;
      return new JBIterable<T>() {
        @Override
        public Iterator<T> iterator() {
          return new JBIterator<T>() {
            T cur = first;

            @Override
            public T nextImpl() {
              if (cur == null) return stop();
              T result = cur;
              cur = paths.get(cur);
              return result;
            }
          };
        }
      };
    }
  }
}
