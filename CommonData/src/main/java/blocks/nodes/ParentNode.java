package blocks.nodes;

import blocks.Item;

import java.util.Iterator;
import java.util.NoSuchElementException;


public class ParentNode extends Node{
    private interface ChildCreator {
        Node create(ParentNode node, NodeCreator creator);
    }
    private final Node[] children = new Node[8];
    private final NodeCreator creator;
    private boolean populated = false;
    private static final ChildCreator[] childCreators = new ChildCreator[8];
    static {
        //layer 1
        childCreators[0] = (n,c) -> c.create(n.minX, n.minY, n.minZ, n.width/2, n.length/2, n.height/2);
        childCreators[1] = (n,c) -> c.create(n.minX + n.width / 2, n.minY, n.minZ, n.width - n.width / 2, n.length/ 2, n.height/ 2);
        childCreators[2] = (n,c) -> c.create(n.minX, n.minY + n.length/ 2, n.minZ, n.width / 2, n.length - n.length/ 2, n.height/ 2);
        childCreators[3] = (n,c) -> c.create(n.minX + n.width / 2, n.minY + n.length/ 2, n.minZ, n.width - n.width / 2, n.length - n.length/ 2, n.height/ 2);
        //layer 2
        childCreators[4] = (n,c) -> c.create(n.minX, n.minY, n.minZ + n.height/ 2, n.width / 2, n.length/ 2, n.height - n.height/ 2);
        childCreators[5] = (n,c) -> c.create(n.minX + n.width / 2, n.minY, n.minZ + n.height/ 2, n.width - n.width / 2, n.length/ 2, n.height - n.height/ 2);
        childCreators[6] = (n,c) -> c.create(n.minX, n.minY + n.length/ 2, n.minZ + n.height/ 2, n.width / 2, n.length - n.length/ 2, n.height - n.height/ 2);
        childCreators[7] = (n,c) -> c.create(n.minX + n.width / 2, n.minY + n.length/ 2, n.minZ + n.height/ 2, n.width - n.width / 2, n.length - n.length/ 2, n.height - n.height/ 2);
    }

    public ParentNode(int minX, int minY, int minZ, int width, int length, int height, int maxSize) {
        super(minX, minY, minZ, width, length, height);

        if ((width / 2) * (length / 2) * (height / 2) > maxSize) {
            this.creator = getParentCreator(maxSize);
        } else {
            this.creator = getLeafCreator();
        }
    }

    private interface NodeCreator {
        Node create(int minX, int minY, int minZ, int width, int length, int height);
    }
    private static NodeCreator getParentCreator(int maxSize) {
        return (minX, minY, minZ, width, length, height) -> new ParentNode(minX, minY, minZ, width, length, height, maxSize);
    }
    private static NodeCreator getLeafCreator() {
        return LeafNode::new;
    }
    @Override
    public int size() {
        if (!populated) return 0;
        int total = 0;
        for (Node child : this.children) {
            if (child == null) continue;
            total += child.size();
        }
        return total;
    }

    @Override
    public boolean groundLayer(Item[][] model, BlockConverter converter) {
        int sum = 0;
        for (int i = 4; i <= 7; i++) {
            Node upperChild = this.children[i];
            if (!upperChild.groundLayer(model, converter)) {
                sum += this.children[i-4].groundLayer(model, converter) ? 1 : 0;
            } else {
                sum++;
            }
        }
        return sum == 4;
    }

    @Override
    public void forEach(ByteAction action) {
        if (!populated) return;
        for (Node child : this.children) {
            if (child == null) continue;
            child.forEach(action);
        }
    }

    @Override
    public void forEach(ByteAction action, BBox box) {
        for (Node child : children) {
            if (child == null) continue;
            if (child.inside(box)) {
                child.forEach(action, box);
            }
        }
    }

    private int rightChild(int x, int y, int z) {
        int childWidth = this.width / 2;
        int childLength = this.length / 2;
        int childHeight = this.height / 2;
        return (z >= minZ + childHeight ? 4 : 0) + (y >= minY + childLength ? 2 : 0) + (x >= minX + childWidth ? 1 : 0);
    }
    private Node populate(int x, int y, int z) {
        this.populated = true;
        int i = rightChild(x,y,z);
        Node child = children[i];
        if (child != null) return child;

        return children[i] = childCreators[i].create(this, creator);
            /*
            x -= minX;
            y -= minY;
            z -= minZ;
            int childWidth = this.width / 2;
            int childLength = this.length / 2;
            int childHeight = this.height / 2;
            NodeCreator creator;
            if ((childWidth) * (childLength) * (childHeight) > maxSize) {
                creator = getParentCreator(maxSize);
            } else {
                creator = getLeafCreator();
            }
            if (z < childHeight) {
                //layer 1
                if (x < childWidth && y < childLength) {
                    return children[0] = creator.create(minX, minY, minZ, childWidth, childLength, childHeight);
                }
                else if (y < childLength) {
                    return children[1] = creator.create(minX + childWidth, minY, minZ, this.width - childWidth, childLength, childHeight);
                }
                else if (x < childWidth) {
                    return children[2] = creator.create(minX, minY + childLength, minZ, childWidth, this.length - childLength, childHeight);
                } else {
                    return children[3] = creator.create(minX + childWidth, minY + childLength, minZ, this.width - childWidth, this.length - childLength, childHeight);
                }
            } else {
                //layer 2
                if (x < childWidth && y < childLength) {
                    return children[4] = creator.create(minX, minY, minZ + childHeight, childWidth, childLength, this.height - childHeight);
                }
                else if (y < childLength) {
                    return children[5] = creator.create(minX + childWidth, minY, minZ + childHeight, this.width - childWidth, childLength, this.height - childHeight);
                }
                else if (x < childWidth) {
                    return children[6] = creator.create(minX, minY + childLength, minZ + childHeight, childWidth, this.length - childLength, this.height - childHeight);
                } else {
                    return children[7] = creator.create(minX + childWidth, minY + childLength, minZ + childHeight, this.width - childWidth, this.length - childLength, this.height - childHeight);
                }
            }

             */
    }

    @Override
    public byte get(int x, int y, int z) {
        if (outOfBounds(x, y, z)) return 0;
        if (!populated) return 0;

        return children[rightChild(x,y,z)].get(x,y,z);
    }

    @Override
    public boolean set(int x, int y, int z, byte block) {
        if (outOfBounds(x, y, z)) return false;
        Node child = this.populate(x, y, z);
        return child.set(x,y,z,block);
    }
    private class ParentIterator implements Iterator<ByteItem> {
        private int childIndex = 0;
        private Iterator<ByteItem> currentIterator;
        private void reset() {
            if (!populated) return;
            this.childIndex = 0;
            this.currentIterator = children[childIndex] != null ? children[childIndex].iterator() : null;
            advance();
        }
        private void advance() {
            while (currentIterator == null || !currentIterator.hasNext()) {
                childIndex++;
                if (childIndex >= children.length) break;
                currentIterator = children[childIndex] != null ? children[childIndex].iterator() : null;
            }
        }
        @Override
        public boolean hasNext() {
            if (!populated) return false;
            if (childIndex >= children.length) return false;
            return currentIterator.hasNext();
        }

        @Override
        public ByteItem next() {
            if (childIndex >= children.length)
                throw new NoSuchElementException();
            ByteItem child = currentIterator.next();
            advance();
            return child;
        }
    }
    private final ParentIterator iterator = new ParentIterator();
    @Override
    public Iterator<ByteItem> iterator() {
        iterator.reset();
        return iterator;
    }
}