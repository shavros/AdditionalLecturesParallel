package CW1;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class QuickSortMultiThreading extends RecursiveTask<Integer> {

    int start, end;
    int[] arr;

    private int partion(int start, int end, int[] arr)
    {

        int i = start, j = end;
        int pivote = new Random().nextInt(j - i) + i;
        int t = arr[j];
        arr[j] = arr[pivote];
        arr[pivote] = t;
        j--;
        while (i <= j) {
            if (arr[i] <= arr[end]) {
                i++;
                continue;
            }
            if (arr[j] >= arr[end]) {
                j--;
                continue;
            }
            t = arr[j];
            arr[j] = arr[i];
            arr[i] = t;
            j--;
            i++;
        }
        t = arr[j + 1];
        arr[j + 1] = arr[end];
        arr[end] = t;
        return j + 1;
    }

    public QuickSortMultiThreading(int start, int end, int[] arr)
    {
        this.arr = arr;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute()
    {
        if (start >= end)
            return null;

        int p = partion(start, end, arr);

        QuickSortMultiThreading left = new QuickSortMultiThreading(start, p - 1, arr);

        QuickSortMultiThreading right = new QuickSortMultiThreading(p + 1, end, arr);


        left.fork();
        right.compute();
        left.join();
        return null;
    }

    public static void main(String args[])
    {
        int n = 10;
        int[] arr = { 54, 64, 95, 82, 107, 12, 32, 63, 7, 45 };
        System.out.println("input:");
        for (int i = 0; i < n; i++)
            System.out.print(arr[i] + " ");

        ForkJoinPool pool
                = ForkJoinPool.commonPool();


        pool.invoke(
                new QuickSortMultiThreading(
                        0, n - 1, arr));

        System.out.println("");
        System.out.println("output:");
        for (int i = 0; i < n; i++)
            System.out.print(arr[i] + " ");
    }
}
