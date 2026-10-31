import { useState } from 'react'
import { flexRender, getCoreRowModel, getPaginationRowModel, useReactTable, type ColumnDef } from '@tanstack/react-table'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Button } from '@/components/ui/button'

export function DataTable<T>({ data, columns, serverPage }: {
  data: T[]; columns: ColumnDef<T>[];
  serverPage?: { page: number; size: number; totalPages: number; change: (page: number) => void };
}) {
  const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 })
  const current = serverPage ? { pageIndex: serverPage.page, pageSize: serverPage.size } : pagination
  const table = useReactTable({ data, columns, getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: serverPage ? undefined : getPaginationRowModel(),
    manualPagination: !!serverPage, pageCount: serverPage?.totalPages, state: { pagination: current },
    onPaginationChange: updater => {
      const next = typeof updater === 'function' ? updater(current) : updater
      if (serverPage) serverPage.change(next.pageIndex); else setPagination(next)
    },
  })
  return <><Table><TableHeader>{table.getHeaderGroups().map(group => <TableRow key={group.id}>{group.headers.map(header => <TableHead key={header.id}>{header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}</TableHead>)}</TableRow>)}</TableHeader>
    <TableBody>{table.getRowModel().rows.length ? table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>) : <TableRow><TableCell colSpan={columns.length} className="h-24 text-center">No records on this page.</TableCell></TableRow>}</TableBody></Table>
    {table.getPageCount() > 1 && <div className="pagination-controls"><Button variant="outline" disabled={!table.getCanPreviousPage()} onClick={() => table.previousPage()}>Previous</Button><span>Page {current.pageIndex + 1} of {table.getPageCount()}</span><Button variant="outline" disabled={!table.getCanNextPage()} onClick={() => table.nextPage()}>Next</Button></div>}
  </>
}
